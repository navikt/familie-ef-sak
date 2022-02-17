package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.FagsakMedPerson
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
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SimuleringService(private val iverksettClient: IverksettClient,
                        private val behandlingService: BehandlingService,
                        private val fagsakService: FagsakService,
                        private val vedtakService: VedtakService,
                        private val blankettSimuleringsService: BlankettSimuleringsService,
                        private val simuleringsresultatRepository: SimuleringsresultatRepository,
                        private val tilkjentYtelseService: TilkjentYtelseService,
                        private val tilgangService: TilgangService) {


    fun simuler(behandlingId: UUID): Simuleringsoppsummering {
        val behandling = behandlingService.hentBehandling(behandlingId)

        return when (behandling.type) {
            BehandlingType.BLANKETT -> simulerForBlankett(behandling)
            else -> simulerForBehandling(behandling)
        }
    }

    fun hentLagretSimuleringsresultat(behandlingId: UUID): Simuleringsoppsummering {
        val simuleringsresultat: Simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandlingId)
        return simuleringsresultat.beriketData.oppsummering
    }

    fun slettSimuleringForBehandling(behandlingId: UUID) {
        simuleringsresultatRepository.deleteById(behandlingId)
    }

    fun hentOgLagreSimuleringsresultat(behandling: Behandling): Simuleringsresultat {
        tilgangService.validerHarSaksbehandlerrolle()
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(behandling.fagsakId)
        simuleringsresultatRepository.deleteById(behandling.id)
        val beriketSimuleringsresultat = simulerMedTilkjentYtelse(behandling, fagsak)
        return simuleringsresultatRepository.insert(Simuleringsresultat(
                behandlingId = behandling.id,
                data = beriketSimuleringsresultat.detaljer,
                beriketData = beriketSimuleringsresultat
        ))
    }

    private fun simulerForBehandling(behandling: Behandling): Simuleringsoppsummering {

        if (behandling.status.behandlingErLåstForVidereRedigering() || !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)) {
            val simuleringsresultat: Simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)
            return simuleringsresultat.beriketData.oppsummering
        }
        val simuleringsresultat = hentOgLagreSimuleringsresultat(behandling)
        return simuleringsresultat.beriketData.oppsummering
    }

    private fun simulerMedTilkjentYtelse(behandling: Behandling, fagsakMedPerson: FagsakMedPerson): BeriketSimuleringsresultat {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)

        val tilkjentYtelseMedMedtadata =
                tilkjentYtelse.tilTilkjentYtelseMedMetaData(saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                                                            eksternBehandlingId = behandling.eksternId.id,
                                                            stønadstype = fagsakMedPerson.stønadstype,
                                                            eksternFagsakId = fagsakMedPerson.eksternId.id)

        return iverksettClient.simuler(SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseMedMedtadata,
                forrigeBehandlingId = behandling.forrigeBehandlingId
        ))
    }

    private fun simulerForBlankett(behandling: Behandling): Simuleringsoppsummering {
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val vedtak = vedtakService.hentVedtakHvisEksisterer(behandling.id)
        val tilkjentYtelseForBlankett = blankettSimuleringsService.genererTilkjentYtelseForBlankett(vedtak, behandling, fagsak)
        val simuleringDto = SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseForBlankett,
                forrigeBehandlingId = null

        )
        return iverksettClient.simuler(simuleringDto).oppsummering
    }
}