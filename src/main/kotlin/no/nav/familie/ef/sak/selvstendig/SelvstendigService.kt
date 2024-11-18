package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.amelding.InntektService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.kuttPeriodeTilGittÅr
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.ef.sak.sigrun.SigrunService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class SelvstendigService(
    val oppgaveService: OppgaveService,
    val fagsakService: FagsakService,
    val behandlingService: BehandlingService,
    val vedtakService: VedtakService,
    val sigrunService: SigrunService,
    val inntektService: InntektService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentOppgaver() {
        if (LeaderClient.isLeader() == true) {
            val førsteOppgave = hentFørsteOppgave()
            secureLogger.info("Første oppgave: $førsteOppgave")
            val personIdent =
                førsteOppgave?.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                    ?: førsteOppgave?.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${førsteOppgave?.id}")

            secureLogger.info("Kontroller person med ident: $personIdent")
            val fagsaker = fagsakService.finnFagsaker(setOf(personIdent))
            val fagsakId = fagsaker.filter { it.stønadstype == StønadType.OVERGANGSSTØNAD }.map { it.id }.firstOrNull() ?: throw Exception("Fant ikke OS behandling")

            val årstallIFjor = YearMonth.now().minusYears(1).year
            validerVedtakLengerEnn4MndForÅr(behandlingService.hentBehandlinger(fagsakId).first(), årstallIFjor)

            if (validerHarLavNæringsinntekt(fagsaker, årstallIFjor)) return

            val fjoråretsInntekt = inntektService.hentÅrsinntekt(personIdent, årstallIFjor)
            secureLogger.info("Forrige års inntekt for person uten ytelse fra offentlig: $fjoråretsInntekt")
            if (fjoråretsInntekt == 0) {
                secureLogger.info("Kan håndteres")
            }
        }
    }

    private fun validerHarLavNæringsinntekt(
        fagsaker: List<Fagsak>,
        årstallIFjor: Int,
    ): Boolean {
        val fagsakPersonId = fagsaker.filter { it.stønadstype == StønadType.OVERGANGSSTØNAD }.first().fagsakPersonId
        val inntekt = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId).filter { it.inntektsår == årstallIFjor }

        val næringsinntekt = inntekt.sumOf { it.næring } + inntekt.sumOf { it.svalbard?.næring ?: 0 }
        secureLogger.info("Inntekt for person $inntekt - næringsinntekt er beregnet til: $næringsinntekt")

        if (næringsinntekt < 50_000) {
            return true
        }
        return false
    }

    private fun hentFørsteOppgave(): Oppgave? {
        val mappeIdForSelvstendigNæringsdrivende = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { it.navn == "61 Selvstendig næringsdrivende" }.id
        secureLogger.info("MappeId for selvstendige: $mappeIdForSelvstendigNæringsdrivende")
        logger.info("MappeId for selvstendige: $mappeIdForSelvstendigNæringsdrivende")
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.ENF,
                behandlingstema = Behandlingstema.Overgangsstønad,
                oppgavetype = Oppgavetype.Fremlegg,
                fristTomDato = LocalDate.of(YearMonth.now().year, 12, 15),
                mappeId = mappeIdForSelvstendigNæringsdrivende.toLong(),
            )
        val oppgaverForSelvstendige = oppgaveService.hentOppgaver(finnOppgaveRequest)
        secureLogger.info("Antall oppgaver for selvstendige med frist 15. desember: ${oppgaverForSelvstendige.oppgaver.size}")
        secureLogger.info("Oppgave for selvstendig: ${oppgaverForSelvstendige.oppgaver.firstOrNull()}")
        if (oppgaverForSelvstendige.oppgaver.isEmpty() ||
            oppgaverForSelvstendige.oppgaver
                .first()
                .identer
                ?.any { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT } == false
        ) {
            secureLogger.info("fant ingen oppgaver for selvstendige med frist 15. desember")
            return null
        }
        return oppgaverForSelvstendige.oppgaver.firstOrNull()
    }

    private fun validerVedtakLengerEnn4MndForÅr(
        behandling: Behandling,
        år: Int,
    ): Boolean {
        val vedtak = vedtakService.hentVedtak(behandling.id)

        val vedtaksperioder = vedtak.perioder?.perioder?.filter { !it.periodeType.midlertidigOpphørEllerSanksjon() }
        val vedtaksperioderIÅr = vedtaksperioder?.filter { it.periode.overlapper(Månedsperiode(YearMonth.of(YearMonth.now().year, 1), YearMonth.of(YearMonth.now().year, 12))) }

        val antallMåneder = vedtaksperioderIÅr?.map { it.periode.kuttPeriodeTilGittÅr(år) }?.sumOf { it.lengdeIHeleMåneder() } ?: 0

        // Beregn antall måneder
        secureLogger.info("Antall måneder $antallMåneder med vedtak i ${YearMonth.now().year} for $behandling")

        if (antallMåneder < 4L) {
            return false
        }
        return true
    }
}
