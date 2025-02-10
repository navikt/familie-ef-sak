package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.domain.SamværsukeWrapper
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilDomene
import org.springframework.http.HttpStatus
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
    fun opprettEllerErstatt(request: SamværsavtaleDto): Samværsavtale {
        val behandling = behandlingService.hentBehandling(request.behandlingId)
        val behandlingBarn = barnService.finnBarnPåBehandling(request.behandlingId)

        validerBehandling(behandling)
        validerRequest(request, behandlingBarn)

        val lagretSamværsavtale = hentSamværsavtaleEllerNull(request.behandlingId, request.behandlingBarnId)

        return if (lagretSamværsavtale === null) {
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

    private fun hentSamværsavtaleEllerNull(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    ): Samværsavtale? = samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId)

    private fun validerBehandling(behandling: Behandling) {
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            throw ApiFeil("Behandlingen er låst for videre redigering", HttpStatus.BAD_REQUEST)
        }
        if (!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandling.id)) {
            throw ApiFeil("Behandlingen eies av en annen saksbehandler", HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerRequest(
        request: SamværsavtaleDto,
        behandlingBarn: List<BehandlingBarn>,
    ) {
        if (request.mapTilSamværsandelerPerDag().any { samværsandeler -> samværsandeler.size > samværsandeler.toSet().size }) {
            throw Feil(
                "Kan ikke ha duplikate samværsandeler innenfor en og samme dag. BehandlingId=${request.behandlingId}",
                "Kan ikke ha duplikate samværsandeler innenfor en og samme dag.",
                HttpStatus.BAD_REQUEST,
            )
        }

        if (request.summerTilSamværsandelerVerdiPerDag().any { it > 8 }) {
            throw Feil(
                "Kan ikke ha mer enn 8 samværsandeler per dag. BehandlingId=${request.behandlingId}",
                "Kan ikke ha mer enn 8 samværsandeler per dag.",
                HttpStatus.BAD_REQUEST,
            )
        }

        if (!behandlingBarn.map { it.id }.contains(request.behandlingBarnId)) {
            throw Feil(
                "Kan ikke opprette en samværsavtale for et barn som ikke eksisterer på behandlingen. BehandlingId=${request.behandlingId}",
                "Kan ikke opprette en samværsavtale for et barn som ikke eksisterer på behandlingen.",
                HttpStatus.BAD_REQUEST,
            )
        }
    }
}
