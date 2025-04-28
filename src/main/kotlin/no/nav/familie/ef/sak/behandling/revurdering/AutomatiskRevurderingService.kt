package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class AutomatiskRevurderingService(
    private val sigrunService: SigrunService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val aMeldingInntektClient: AMeldingInntektClient,
    private val grunnlagsdataInntektRepository: GrunnlagsdataInntektRepository,
    private val vedtakService: VedtakService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun kanAutomatiskRevurderes(personIdent: String): Boolean {
        val fagsak = fagsakService.finnFagsak(setOf(personIdent), StønadType.OVERGANGSSTØNAD) ?: return false
        logger.info("Sjekker om fagsak ${fagsak.id} automatisk revurderes")
        val inntektForAlleÅr = sigrunService.hentInntektForAlleÅrMedInntekt(fagsak.fagsakPersonId)

        if (inntektForAlleÅr.harNæringsinntekt()) {
            logger.info("Har næringsinntekt for fagsak ${fagsak.id}")
            secureLogger.info("Har næringsinntekt for fagsak ${fagsak.id} - InntektResponse: $inntektForAlleÅr")
            return false
        }

        if (behandlingService.finnesÅpenBehandling(fagsak.id)) {
            logger.info("Finnes åpen behandling for fagsak ${fagsak.id}")
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

        val sisteIverksatteBehandlingId = behandlingService.finnSisteIverksatteBehandling(fagsak.id)?.id
        if (sisteIverksatteBehandlingId == null) {
            logger.error("Fant ikke siste iverksatte behandling for fagsakId: ${fagsak.id}")
            return false
        }

        val vedtak = vedtakService.hentVedtak(sisteIverksatteBehandlingId)
        if (vedtak.perioder?.perioder?.size != 1 && vedtak.inntekter?.inntekter?.size != 1) { // Denne valideringen kan fjernes når logikken for å sette revurderes fra-dato er forbedret
            logger.info("behandlingId: $sisteIverksatteBehandlingId har flere vedtaksperioder og kan derfor ikke automatisk revurderes")
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
