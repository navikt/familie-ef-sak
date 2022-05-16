package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.tilTilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SimuleringService(private val iverksettClient: IverksettClient,
                        private val vedtakService: VedtakService,
                        private val blankettSimuleringsService: BlankettSimuleringsService,
                        private val simuleringsresultatRepository: SimuleringsresultatRepository,
                        private val tilkjentYtelseService: TilkjentYtelseService,
                        private val tilgangService: TilgangService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun simuler(saksbehandling: Saksbehandling): Simuleringsoppsummering {
        return when (saksbehandling.type) {
            BehandlingType.BLANKETT -> simulerForBlankett(saksbehandling)
            else -> simulerForBehandling(saksbehandling)
        }
    }

    fun hentLagretSimuleringsoppsummering(behandlingId: UUID): Simuleringsoppsummering {
        return hentLagretSimmuleringsresultat(behandlingId).oppsummering
    }

    fun hentLagretSimmuleringsresultat(behandlingId: UUID): BeriketSimuleringsresultat {
        return simuleringsresultatRepository.findByIdOrThrow(behandlingId).beriketData
    }

    fun slettSimuleringForBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette simulering for behandling=$behandlingId då den er låst"
        }
        logger.info("Sletter simulering for behandling=$behandlingId")
        simuleringsresultatRepository.deleteById(behandlingId)
    }

    fun hentOgLagreSimuleringsresultat(saksbehandling: Saksbehandling): Simuleringsresultat {
        tilgangService.validerHarSaksbehandlerrolle()

        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke hente og lagre simuleringsresultat då behandling=${saksbehandling.id} er låst"
        }

        val beriketSimuleringsresultat = simulerMedTilkjentYtelse(saksbehandling)
        simuleringsresultatRepository.deleteById(saksbehandling.id)
        return simuleringsresultatRepository.insert(Simuleringsresultat(
                behandlingId = saksbehandling.id,
                data = beriketSimuleringsresultat.detaljer,
                beriketData = beriketSimuleringsresultat
        ))
    }

    private fun simulerForBehandling(saksbehandling: Saksbehandling): Simuleringsoppsummering {

        if (saksbehandling.status.behandlingErLåstForVidereRedigering()
            || !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)) {
            return hentLagretSimuleringsoppsummering(saksbehandling.id)
        }
        val simuleringsresultat = hentOgLagreSimuleringsresultat(saksbehandling)
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
        } catch (e: Exception) {
            val personFinnesIkkeITps = "Personen finnes ikke i TPS"
            brukerfeilHvis(e is RessursException && e.ressurs.melding == personFinnesIkkeITps) {
                personFinnesIkkeITps
            }
            throw Feil(message = "Kunne ikke utføre simulering",
                       frontendFeilmelding = "Kunne ikke utføre simulering. Vennligst prøv på nytt",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                       throwable = e)
        }
    }

    private fun simulerForBlankett(saksbehandling: Saksbehandling): Simuleringsoppsummering {
        val vedtak = vedtakService.hentVedtakHvisEksisterer(saksbehandling.id)
        val tilkjentYtelseForBlankett = blankettSimuleringsService.genererTilkjentYtelseForBlankett(vedtak, saksbehandling)
        val simuleringDto = SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseForBlankett,
                forrigeBehandlingId = null

        )
        return iverksettClient.simuler(simuleringDto).oppsummering
    }
}