package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class SimuleringService(private val iverksettClient: IverksettClient,
                        private val behandlingService: BehandlingService,
                        private val fagsakService: FagsakService,
                        private val vedtakService: VedtakService,
                        private val blankettSimuleringsService: BlankettSimuleringsService,
                        private val simuleringsresultatRepository: SimuleringsresultatRepository,
                        private val tilkjentYtelseService: TilkjentYtelseService) {


    fun simuler(behandlingId: UUID): SimuleringsresultatDto {
        val behandling = behandlingService.hentBehandling(behandlingId)

        return when (behandling.type) {
            BehandlingType.BLANKETT -> simulerForBlankett(behandling)
            else -> simulerForBehandling(behandling)
        }
    }

    fun hentLagretSimuleringsresultat(behandlingId: UUID): SimuleringsresultatDto {
        val simuleringsresultat: Simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandlingId)
        return tilSimuleringsresultatDto(simuleringsresultat.data, simuleringsresultat.sporbar.endret.endretTid.toLocalDate())
    }

    fun slettSimuleringForBehandling(behandlingId: UUID) {
        simuleringsresultatRepository.deleteById(behandlingId)
    }

    fun hentOgLagreSimuleringsresultat(behandling: Behandling): Simuleringsresultat {
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        simuleringsresultatRepository.deleteById(behandling.id)
        val detaljertSimuleringResultat = simulerMedTilkjentYtelse(behandling, fagsak)
        val simuleringsoppsummering = tilSimuleringsresultatDto(detaljertSimuleringResultat, LocalDate.now()).tilSimuleringsperiode()
        return simuleringsresultatRepository.insert(Simuleringsresultat(
                behandlingId = behandling.id,
                data = detaljertSimuleringResultat,
                beriketData = BeriketSimuleringsresultat(detaljertSimuleringResultat, simuleringsoppsummering)
        ))
    }

    private fun simulerForBehandling(behandling: Behandling): SimuleringsresultatDto {

        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            val simuleringsresultat: Simuleringsresultat = simuleringsresultatRepository.findByIdOrThrow(behandling.id)
            return tilSimuleringsresultatDto(simuleringsresultat.data, simuleringsresultat.sporbar.opprettetTid.toLocalDate())
        }

        val simuleringsresultat = hentOgLagreSimuleringsresultat(behandling)
        return tilSimuleringsresultatDto(simuleringsresultat.data, LocalDate.now())
    }

    private fun simulerMedTilkjentYtelse(behandling: Behandling, fagsak: Fagsak): DetaljertSimuleringResultat {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)

        val tilkjentYtelseMedMedtadata =
                tilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                                                       eksternBehandlingId = behandling.eksternId.id,
                                                       stønadstype = fagsak.stønadstype,
                                                       eksternFagsakId = fagsak.eksternId.id)

        return iverksettClient.simuler(SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseMedMedtadata,
                forrigeBehandlingId = behandling.forrigeBehandlingId
        ))
    }

    private fun simulerForBlankett(behandling: Behandling): SimuleringsresultatDto {
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val vedtak = vedtakService.hentVedtakHvisEksisterer(behandling.id)
        val tilkjentYtelseForBlankett = blankettSimuleringsService.genererTilkjentYtelseForBlankett(vedtak, behandling, fagsak)
        val simuleringDto = SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseForBlankett,
                forrigeBehandlingId = null

        )
        return tilSimuleringsresultatDto(iverksettClient.simuler(simuleringDto), LocalDate.now())
    }


}