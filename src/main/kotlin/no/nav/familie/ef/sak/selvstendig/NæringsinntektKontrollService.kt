package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.amelding.InntektService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
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
import java.util.UUID

@Service
class NæringsinntektKontrollService(
    val oppgaveService: OppgaveService,
    val fagsakService: FagsakService,
    val behandlingService: BehandlingService,
    val vedtakService: VedtakService,
    val sigrunService: SigrunService,
    val inntektService: InntektService,
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val årstallIFjor = YearMonth.now().minusYears(1).year

    fun sjekkNæringsinntektMotForventetInntekt() {
        if (LeaderClient.isLeader() == true) {
            val oppgaver = hentOppgaverForSelvstendigeTilInntektskontroll()

            oppgaver.forEach { oppgave ->
                val personIdent =
                    oppgave.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                        ?: oppgave.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${oppgave.id}")

                secureLogger.info("Kontrollerer person med ident: $personIdent")

                val fagsakOS = fagsakService.finnFagsaker(setOf(personIdent)).filter { it.stønadstype == StønadType.OVERGANGSSTØNAD }.firstOrNull() ?: throw RuntimeException("Fant ikke fagsak for overgangsstønad for person: $personIdent")
                val behandling = behandlingService.finnSisteIverksatteBehandling(fagsakOS.id) ?: throw RuntimeException("Fant ikke behandling for fagsakId: ${fagsakOS.id}")
                val antallMånederMedVedtakForFjoråret = antallMånederMedVedtakForFjoråret(behandling)

                val næringsinntekt = hentFjoråretsNæringsinntekt(fagsakOS.fagsakPersonId)

                if (antallMånederMedVedtakForFjoråret > 3 && næringsinntekt > 50_000) {
                    val fjoråretsPersoninntekt = inntektService.hentÅrsinntekt(personIdent, årstallIFjor)
                    secureLogger.info("Forrige års inntekt for person uten ytelse fra offentlig: $fjoråretsPersoninntekt")
                    if (fjoråretsPersoninntekt == 0) {
                        val forventetInntekt = forventetInntektSnitt(behandling.id)
                        secureLogger.info("Beregnet inntekt i snitt for år $årstallIFjor og behandlingId ${behandling.id} er: $forventetInntekt")
                        if (næringsinntekt > (forventetInntekt * 1.1)) {
                            secureLogger.info("Har 10% høyere næringsinntekt for person: $personIdent (Næringsinntekt: $næringsinntekt - ForventetInntekt: $forventetInntekt)")
                        }
                    }
                }
            }
        }
    }

    private fun hentFjoråretsNæringsinntekt(fagsakPersonId: UUID): Int {
        val inntekt = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId).filter { it.inntektsår == årstallIFjor }

        val næringsinntekt = inntekt.sumOf { it.næring } + inntekt.sumOf { it.svalbard?.næring ?: 0 }
        secureLogger.info("Inntekt for person $inntekt - næringsinntekt er beregnet til: $næringsinntekt")
        return næringsinntekt
    }

    private fun forventetInntektSnitt(behandlingId: UUID): Int {
        val vedtak = vedtakService.hentVedtak(behandlingId)

        val inntektsperioderIÅr = vedtak.inntekter?.inntekter?.filter { it.periode.overlapper(Månedsperiode(YearMonth.of(årstallIFjor, 1), YearMonth.of(årstallIFjor, 12))) }
        val totalInntekt = inntektsperioderIÅr?.sumOf { it.totalinntekt() }

        val antallPerioder = inntektsperioderIÅr?.size ?: throw RuntimeException("Fant ikke inntektsperiode på vedtak for år $årstallIFjor for behandling $behandlingId")
        val forventetInntektSnitt = (totalInntekt?.toInt() ?: 1) / antallPerioder

        return forventetInntektSnitt
    }

    private fun hentOppgaverForSelvstendigeTilInntektskontroll(): List<Oppgave> {
        val mappeIdForSelvstendigNæringsdrivende = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { it.navn == "61 Selvstendig næringsdrivende" }.id
        secureLogger.info("MappeId for selvstendige: $mappeIdForSelvstendigNæringsdrivende")
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

        if (oppgaverForSelvstendige.oppgaver.isEmpty() ||
            oppgaverForSelvstendige.oppgaver
                .first()
                .identer
                ?.any { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT } == false
        ) {
            secureLogger.info("Fant ingen oppgaver for selvstendige med frist 15. desember")
            return emptyList()
        }
        return oppgaverForSelvstendige.oppgaver
    }

    private fun antallMånederMedVedtakForFjoråret(
        behandling: Behandling,
    ): Int {
        val vedtak = vedtakService.hentVedtak(behandling.id)

        val vedtaksperioder = vedtak.perioder?.perioder?.filter { !it.periodeType.midlertidigOpphørEllerSanksjon() }
        val vedtaksperioderIÅr = vedtaksperioder?.filter { it.periode.overlapper(Månedsperiode(YearMonth.of(årstallIFjor, 1), YearMonth.of(årstallIFjor, 12))) }

        val antallMåneder = vedtaksperioderIÅr?.map { it.periode.kuttPeriodeTilGittÅr(årstallIFjor) }?.sumOf { it.lengdeIHeleMåneder() } ?: 0

        // Beregn antall måneder
        secureLogger.info("Antall måneder $antallMåneder med vedtak i ${YearMonth.now().year} for $behandling")

        return antallMåneder.toInt()
    }
}
