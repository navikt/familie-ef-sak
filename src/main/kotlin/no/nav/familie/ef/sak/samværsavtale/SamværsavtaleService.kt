package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.domain.SamværsukeWrapper
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilDomene
import no.nav.familie.ef.sak.vilkår.VurderingService.Companion.byggBarnMapFraTidligereTilNyId
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SamværsavtaleService(
    val samværsavtaleRepository: SamværsavtaleRepository,
    val behandlingService: BehandlingService,
    val tilordnetRessursService: TilordnetRessursService,
    val barnService: BarnService,
) {
    fun hentSamværsavtalerForBehandling(behandlingId: UUID) = samværsavtaleRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun opprettEllerErstattSamværsavtale(request: SamværsavtaleDto): Samværsavtale {
        val behandling = behandlingService.hentBehandling(request.behandlingId)
        val behandlingBarn = barnService.finnBarnPåBehandling(request.behandlingId)

        validerBehandling(behandling)
        validerRequest(request, behandlingBarn)

        val lagretSamværsavtale = hentSamværsavtaleEllerNull(request.behandlingId, request.behandlingBarnId)

        return if (lagretSamværsavtale == null) {
            samværsavtaleRepository.insert(request.tilDomene())
        } else {
            samværsavtaleRepository.update(lagretSamværsavtale.copy(uker = SamværsukeWrapper(uker = request.uker)))
        }
    }

    @Transactional
    fun slettSamværsavtale(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerBehandling(behandling)
        samværsavtaleRepository.deleteByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId)
    }

    @Transactional
    fun kopierSamværsavtalerTilNyBehandling(
        eksisterendeBehandlingId: UUID,
        nyBehandlingId: UUID,
        metadata: HovedregelMetadata,
    ) {
        val eksisterendeSamværsavtaler =
            hentSamværsavtalerForBehandling(eksisterendeBehandlingId).associateBy { it.behandlingBarnId }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(eksisterendeBehandlingId)
        val barnIdMap = byggBarnMapFraTidligereTilNyId(barnPåForrigeBehandling, metadata.barn)

        val nyeSamværsavtaler =
            barnIdMap.mapNotNull { (forrigeBehandlingBarnId, nåværendeBehandlingBarn) ->
                eksisterendeSamværsavtaler.get(forrigeBehandlingBarnId)?.copy(
                    id = UUID.randomUUID(),
                    behandlingId = nyBehandlingId,
                    behandlingBarnId = nåværendeBehandlingBarn.id,
                )
            }

        samværsavtaleRepository.insertAll(nyeSamværsavtaler)
    }

    private fun hentSamværsavtaleEllerNull(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    ): Samværsavtale? = samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId)

    private fun validerBehandling(behandling: Behandling) {
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst for videre redigering"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandling.id)) {
            "Behandlingen eies av en annen saksbehandler"
        }
    }

    private fun validerRequest(
        request: SamværsavtaleDto,
        behandlingBarn: List<BehandlingBarn>,
    ) {
        brukerfeilHvis(
            request
                .mapTilSamværsandelerPerDag()
                .any { samværsandeler -> samværsandeler.size > samværsandeler.toSet().size },
        ) {
            "Kan ikke ha duplikate samværsandeler innenfor en og samme dag. BehandlingId=${request.behandlingId}"
        }
        brukerfeilHvis(request.summerTilSamværsandelerVerdiPerDag().any { it > 8 }) {
            "Kan ikke ha mer enn 8 samværsandeler per dag. BehandlingId=${request.behandlingId}"
        }
        brukerfeilHvis(!behandlingBarn.map { it.id }.contains(request.behandlingBarnId)) {
            "Kan ikke opprette en samværsavtale for et barn som ikke eksisterer på behandlingen. BehandlingId=${request.behandlingId}"
        }
    }
}
