package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingOppryddingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.tilTilkjentYtelseMedMetaData
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class SimuleringService(
    private val iverksettClient: IverksettClient,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val tilbakekrevingOppryddingService: TilbakekrevingOppryddingService,
) {
    private val logger = Logg.getLogger(this::class)

    @Transactional
    fun simuler(saksbehandling: Saksbehandling): Simuleringsoppsummering {
        if (saksbehandling.status.behandlingErLåstForVidereRedigering() ||
            !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER) ||
            !tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(saksbehandling.id)
        ) {
            return hentLagretSimuleringsoppsummering(saksbehandling.id)
        }
        val simuleringsresultat = hentOgLagreSimuleringsresultat(saksbehandling)
        return simuleringsresultat.beriketData.oppsummering
    }

    fun hentLagretSimuleringsoppsummering(behandlingId: UUID): Simuleringsoppsummering = hentLagretSimmuleringsresultat(behandlingId).oppsummering

    fun hentLagretSimmuleringsresultat(behandlingId: UUID): BeriketSimuleringsresultat {
        val simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandlingId)
        return simuleringsresultat.beriketData
    }

    fun slettSimuleringForBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette simulering for behandling=$behandlingId då den er låst"
        }
        logger.vanligInfo("Sletter simulering for behandling=$behandlingId")
        simuleringsresultatRepository.deleteById(behandlingId)
        tilbakekrevingOppryddingService.slettTilbakekrevingsvalg(behandlingId)
    }

    @Transactional
    fun hentOgLagreSimuleringsresultat(saksbehandling: Saksbehandling): Simuleringsresultat {
        tilgangService.validerHarSaksbehandlerrolle()
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke hente og lagre simuleringsresultat då behandling=${saksbehandling.id} er låst"
        }

        val beriketSimuleringsresultat = simulerMedTilkjentYtelse(saksbehandling)
        tilbakekrevingOppryddingService.slettTilbakekrevingsvalgHvisIngenFeilutbetalingEllerForskjelligBeløp(saksbehandling.id, beriketSimuleringsresultat.oppsummering)
        simuleringsresultatRepository.deleteById(saksbehandling.id)
        return simuleringsresultatRepository.insert(
            Simuleringsresultat(
                behandlingId = saksbehandling.id,
                data = beriketSimuleringsresultat.detaljer,
                beriketData = beriketSimuleringsresultat,
            ),
        )
    }

    fun erSimuleringsoppsummeringEndret(saksbehandling: Saksbehandling): Boolean {
        val lagretSimuleringsoppsummering = hentLagretSimuleringsoppsummering(saksbehandling.id)
        val nySimuleringoppsummering = simulerMedTilkjentYtelse(saksbehandling).oppsummering
        val harUlikFeilutbetaling = lagretSimuleringsoppsummering.feilutbetaling != nySimuleringoppsummering.feilutbetaling
        if (harUlikFeilutbetaling) {
            logger.warn(
                "Behandling med ulikt simuleringsresultat i beslutter-steget. BehandlingId: ${saksbehandling.id} \n" +
                    "lagretSimuleringsoppsummering: $lagretSimuleringsoppsummering \n" +
                    "nySimuleringoppsummering: $nySimuleringoppsummering",
            )
        }

        return harUlikFeilutbetaling
    }

    private fun simulerMedTilkjentYtelse(saksbehandling: Saksbehandling): BeriketSimuleringsresultat {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(saksbehandling.id)

        val tilkjentYtelseMedMedtadata =
            tilkjentYtelse.tilTilkjentYtelseMedMetaData(
                saksbehandlerId = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                eksternBehandlingId = saksbehandling.eksternId,
                stønadstype = saksbehandling.stønadstype,
                eksternFagsakId = saksbehandling.eksternFagsakId,
                vedtaksdato = LocalDate.now(),
            )

        try {
            return iverksettClient.simuler(
                SimuleringDto(
                    nyTilkjentYtelseMedMetaData = tilkjentYtelseMedMedtadata,
                    forrigeBehandlingId = saksbehandling.forrigeBehandlingId,
                ),
            )
        } catch (e: Exception) {
            val personFinnesIkkeITps = "Personen finnes ikke i TPS"
            brukerfeilHvis(e is RessursException && e.ressurs.melding == personFinnesIkkeITps) {
                personFinnesIkkeITps
            }
            throw Feil(
                message = "Kunne ikke utføre simulering",
                frontendFeilmelding = "Kunne ikke utføre simulering. Vennligst prøv på nytt",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e,
            )
        }
    }
}
