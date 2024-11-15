package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
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
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentOppgaver() {
        if (LeaderClient.isLeader() == true) {
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
                return
            }
            val førsteOppgave = oppgaverForSelvstendige.oppgaver.firstOrNull()
            secureLogger.info("Første oppgave: $førsteOppgave")
            val personIdent =
                førsteOppgave?.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                    ?: førsteOppgave?.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${førsteOppgave?.id}")

            secureLogger.info("Kontroller person med ident: $personIdent")
            val fagsaker = fagsakService.finnFagsaker(setOf(personIdent))
            val fagsakId = fagsaker.filter { it.stønadstype == StønadType.OVERGANGSSTØNAD }.map { it.id }.firstOrNull() ?: throw Exception("Fant ikke OS behandling")
            val behandlinger = behandlingService.hentBehandlinger(fagsakId)

            val vedtak = vedtakService.hentVedtak(behandlinger.first().id)

            val perioder = vedtak.perioder?.perioder?.filter { !it.periodeType.midlertidigOpphørEllerSanksjon() }
            val antallMåneder = perioder?.count { it.periode.overlapper(Månedsperiode(YearMonth.of(YearMonth.now().year, 1), YearMonth.of(YearMonth.now().year, 12))) }

            secureLogger.info("Antall måneder $antallMåneder med vedtak i ${YearMonth.now().year} for person $personIdent")

            val fagsakPersonId = fagsaker.filter { it.stønadstype == StønadType.OVERGANGSSTØNAD }.first().fagsakPersonId
            val inntekt = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId)
            secureLogger.info("Inntekt for person $inntekt")
        }
        // har bruker hatt vedtak??
    }
}
