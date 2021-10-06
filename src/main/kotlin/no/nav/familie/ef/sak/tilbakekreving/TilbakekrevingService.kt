package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilbakekreving.domain.tilDto
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.ef.sak.tilbakekreving.dto.tilDomene
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilbakekrevingService(private val tilbakekrevingRepository: TilbakekrevingRepository,
                            private val behandlingService: BehandlingService) {


    fun lagreTilbakekreving(tilbakekrevingDto: TilbakekrevingDto, behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerTilbakekreving(behandling, tilbakekrevingDto)
        tilbakekrevingRepository.deleteById(behandlingId)
        tilbakekrevingRepository.insert(tilbakekrevingDto.tilDomene(behandlingId))
    }

    fun hentTilbakekrevingDto(behandlingId: UUID): TilbakekrevingDto? {
        return tilbakekrevingRepository.findByIdOrNull(behandlingId)?.tilDto()
    }

    private fun validerTilbakekreving(behandling: Behandling, tilbakekrevingDto: TilbakekrevingDto) {
        feilHvis(tilbakekrevingDto.valg == Tilbakekrevingsvalg.OPPRETT_MED_VARSEL && tilbakekrevingDto.varseltekst.isNullOrBlank()) {
            "Må fylle ut varseltekst for å lage tilbakekreving med varsel"
        }
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst for redigering"
        }
    }

}