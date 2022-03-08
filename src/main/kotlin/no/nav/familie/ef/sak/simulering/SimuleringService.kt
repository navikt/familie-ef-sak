package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.tilTilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SimuleringService(private val iverksettClient: IverksettClient,
                        private val vedtakService: VedtakService,
                        private val blankettSimuleringsService: BlankettSimuleringsService,
                        private val simuleringsresultatRepository: SimuleringsresultatRepository,
                        private val tilkjentYtelseService: TilkjentYtelseService,
                        private val tilgangService: TilgangService) {


    fun simuler(saksbehandling: Saksbehandling): Simuleringsoppsummering {
        return when (saksbehandling.type) {
            BehandlingType.BLANKETT -> simulerForBlankett(saksbehandling)
            else -> simulerForBehandling(saksbehandling)
        }
    }

    fun hentLagretSimuleringsresultat(behandlingId: UUID): Simuleringsoppsummering {
        val simuleringsresultat: Simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandlingId)
        return simuleringsresultat.beriketData.oppsummering
    }

    fun slettSimuleringForBehandling(behandlingId: UUID) {
        simuleringsresultatRepository.deleteById(behandlingId)
    }

    fun hentOgLagreSimuleringsresultat(behandling: Saksbehandling): Simuleringsresultat {
        tilgangService.validerHarSaksbehandlerrolle()
        simuleringsresultatRepository.deleteById(behandling.id)
        val beriketSimuleringsresultat = simulerMedTilkjentYtelse(behandling)
        return simuleringsresultatRepository.insert(Simuleringsresultat(
                behandlingId = behandling.id,
                data = beriketSimuleringsresultat.detaljer,
                beriketData = beriketSimuleringsresultat
        ))
    }

    private fun simulerForBehandling(behandling: Saksbehandling): Simuleringsoppsummering {

        if (behandling.status.behandlingErLåstForVidereRedigering()
            || !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)) {
            val simuleringsresultat: Simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)
            return simuleringsresultat.beriketData.oppsummering
        }
        val simuleringsresultat = hentOgLagreSimuleringsresultat(behandling)
        return simuleringsresultat.beriketData.oppsummering
    }

    private fun simulerMedTilkjentYtelse(saksbehandling: Saksbehandling): BeriketSimuleringsresultat {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(saksbehandling.id)

        val tilkjentYtelseMedMedtadata =
                tilkjentYtelse.tilTilkjentYtelseMedMetaData(saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                                                            eksternBehandlingId = saksbehandling.eksternId,
                                                            stønadstype = saksbehandling.stønadstype,
                                                            eksternFagsakId = saksbehandling.eksternFagsakId)

        try {
            return iverksettClient.simuler(SimuleringDto(
                    nyTilkjentYtelseMedMetaData = tilkjentYtelseMedMedtadata,
                    forrigeBehandlingId = saksbehandling.forrigeBehandlingId
            ))
        } catch (exception: Exception) {
            throw Feil(message = "Kunne ikke utføre simulering",
                       frontendFeilmelding = "Kunne ikke utføre simulering. Vennligst prøv på nytt",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = exception)
        }
    }

    private fun simulerForBlankett(behandling: Saksbehandling): Simuleringsoppsummering {
        val vedtak = vedtakService.hentVedtakHvisEksisterer(behandling.id)
        val tilkjentYtelseForBlankett = blankettSimuleringsService.genererTilkjentYtelseForBlankett(vedtak, behandling)
        val simuleringDto = SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseForBlankett,
                forrigeBehandlingId = null

        )
        return iverksettClient.simuler(simuleringDto).oppsummering
    }
}