package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.inntekt.GrunnlagsdataInntekt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.inntekt.GrunnlagsdataInntektRepository
import no.nav.familie.ef.sak.sigrun.SigrunService
import no.nav.familie.ef.sak.sigrun.harNæringsinntekt
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID
import kotlin.collections.contains

@Service
class AutomatiskRevurderingService(
    private val sigrunService: SigrunService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val aMeldingInntektClient: AMeldingInntektClient,
    private val grunnlagsdataInntektRepository: GrunnlagsdataInntektRepository,
    private val vedtakService: VedtakService,
    private val environment: Environment,
    private val arbeidsforholdService: ArbeidsforholdService,
) {
    private val logger = Logg.getLogger(this::class)

    fun kanAutomatiskRevurderes(personIdent: String): Boolean {
        val fagsak = fagsakService.finnFagsak(setOf(personIdent), StønadType.OVERGANGSSTØNAD) ?: return false
        logger.info("Sjekker om fagsak ${fagsak.id} automatisk revurderes")

        val inntektForAlleÅr =
            if (environment.activeProfiles.contains("prod")) {
                sigrunService.hentInntektForAlleÅrMedInntekt(fagsak.fagsakPersonId)
            } else {
                emptyList()
            }

        if (inntektForAlleÅr.harNæringsinntekt()) {
            logger.vanligInfo("Har næringsinntekt for fagsak ${fagsak.id}")
            logger.info("Har næringsinntekt for fagsak ${fagsak.id} - InntektResponse: $inntektForAlleÅr")
            return false
        }

        if (behandlingService.finnesÅpenBehandling(fagsak.id)) {
            logger.info("Finnes åpen behandling for fagsak ${fagsak.id}")
            return false
        }

        if (arbeidsforholdService.finnesAvsluttetArbeidsforholdSisteAntallMåneder(personIdent, 4)) {
            logger.info("Finnes avsluttet arbeidsforhold siste fire måneder for fagsak ${fagsak.id}")
            return false
        }

        if (arbeidsforholdService.finnesNyttArbeidsforholdSisteAntallMåneder(personIdent, 4)) {
            logger.info("Finnes nytt arbeidsforhold siste fire måneder for fagsak ${fagsak.id}")
            return false
        }

        val inntektSisteTreMåneder = aMeldingInntektClient.hentInntekt(personIdent, YearMonth.now().minusMonths(3), YearMonth.now())
        if (inntektSisteTreMåneder.finnesHøyMånedsinntektSomIkkeGirOvergangsstønad) {
            logger.info("Har inntekt over 5.5G for fagsak ${fagsak.id}")
            return false
        }

        val oppgaverForPerson =
            oppgaveService.hentOppgaver(
                FinnOppgaveRequest(
                    aktørId = personIdent,
                    tema = Tema.ENF,
                ),
            )
        val harBehandleSakEllerJournalføringsoppgave =
            oppgaverForPerson.oppgaver.any {
                (it.status == StatusEnum.AAPNET || it.status == StatusEnum.OPPRETTET || it.status == StatusEnum.UNDER_BEHANDLING) &&
                    (it.oppgavetype == Oppgavetype.BehandleSak.toString() || it.oppgavetype == Oppgavetype.Journalføring.toString())
            }

        if (harBehandleSakEllerJournalføringsoppgave) {
            logger.info("harBehandleSakEllerJournalføringsoppgave $oppgaverForPerson")
            return false
        }

        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id)
        if (sisteIverksatteBehandling == null) {
            logger.error("Fant ikke siste iverksatte behandling for fagsakId: ${fagsak.id}")
            return false
        }
        if (sisteIverksatteBehandling.erGOmregning() && sisteIverksatteBehandling.forrigeBehandlingId != null) {
            val vedtak = vedtakService.hentVedtak(sisteIverksatteBehandling.forrigeBehandlingId)
            if (vedtak.perioder?.perioder?.size != 1) { // Denne valideringen kan fjernes når logikken for å sette revurderes fra-dato er forbedret
                logger.info("behandlingId: ${sisteIverksatteBehandling.id} er G-omregning og forrigeBehandling med id ${sisteIverksatteBehandling.forrigeBehandlingId} har flere vedtaksperioder og kan derfor ikke automatisk revurderes")
                return false
            }
        }

        val vedtak = vedtakService.hentVedtak(sisteIverksatteBehandling.id)
        if (vedtak.perioder?.perioder?.size != 1) { // Denne valideringen kan fjernes når logikken for å sette revurderes fra-dato er forbedret
            logger.info("behandlingId: ${sisteIverksatteBehandling.id} har flere vedtaksperioder og kan derfor ikke automatisk revurderes")
            return false
        }

        if (inntektSisteTreMåneder.harMånedMedBareFeriepenger(YearMonth.now().minusMonths(3))) {
            logger.info("Bruker har en måned med bare feriepenger, og skal dermed ikke automatisk revurderes. BehandlingId: ${sisteIverksatteBehandling.id}")
            return false
        }

        return true
    }

    fun lagreInntektResponse(
        personIdent: String,
        behandlingId: UUID,
    ): InntektResponse {
        val inntektResponse = aMeldingInntektClient.hentInntekt(personIdent = personIdent, månedFom = YearMonth.now().minusYears(1), månedTom = YearMonth.now())
        return grunnlagsdataInntektRepository.insert(GrunnlagsdataInntekt(behandlingId, inntektResponse)).inntektsdata
    }

    fun hentInntektResponse(
        personIdent: String,
    ): InntektResponse = aMeldingInntektClient.hentInntekt(personIdent = personIdent, månedFom = YearMonth.now().minusYears(1), månedTom = YearMonth.now())
}
