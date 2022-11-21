package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.tilDomene
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ÅrsakRevurderingService(
    private val behandlingService: BehandlingService,
    private val årsakRevurderingsRepository: ÅrsakRevurderingsRepository
) {
    fun validerHarGyldigRevurderingsinformasjon(saksbehandling: Saksbehandling) {
        if (saksbehandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) {
            val revurderingsinformasjon = hentRevurderingsinformasjon(saksbehandling.id)
            brukerfeilHvis(revurderingsinformasjon.kravMottatt == null || revurderingsinformasjon.årsakRevurdering == null) {
                "Behandlingen mangler årsak til revurdering. fyll inn informasjonen i fanen for årsak revurdering"
            }
        }
    }

    fun hentRevurderingsinformasjon(behandlingId: UUID): RevurderingsinformasjonDto {
        val kravMottatt = behandlingService.hentSaksbehandling(behandlingId).kravMottatt
        val årsakRevurdering = årsakRevurderingsRepository.findByIdOrNull(behandlingId)
        return RevurderingsinformasjonDto(
            kravMottatt = kravMottatt,
            årsakRevurdering = årsakRevurdering?.tilDto(),
            endretTid = årsakRevurdering?.sporbar?.endret?.endretTid
        )
    }

    @Transactional
    fun oppdaterRevurderingsinformasjon(
        saksbehandling: Saksbehandling,
        kravMottatt: LocalDate,
        årsakRevurdering: ÅrsakRevurderingDto
    ) {
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og kan ikke oppdatere revurderingsinformasjon"
        }
        årsakRevurderingsRepository.deleteById(saksbehandling.id)
        årsakRevurderingsRepository.insert(årsakRevurdering.tilDomene(saksbehandling.id))
        behandlingService.oppdaterKravMottatt(saksbehandling.id, kravMottatt)
    }

    @Transactional
    fun slettRevurderingsinformasjon(behandlingId: UUID) {
        årsakRevurderingsRepository.deleteById(behandlingId)
        behandlingService.oppdaterKravMottatt(behandlingId, null)
    }
}
