package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
                "Behandlingen mangler årsak til revurdering. fyll i informasjonen i fanen for årsak revurdering"
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
    fun slettRevurderingsinformasjon(behandlingId: UUID) {
        årsakRevurderingsRepository.deleteById(behandlingId)
        behandlingService.oppdaterKravMottatt(behandlingId, null)
    }
}