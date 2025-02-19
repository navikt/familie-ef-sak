package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.tilDomene
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ÅrsakRevurderingService(
    private val behandlingService: BehandlingService,
    private val årsakRevurderingsRepository: ÅrsakRevurderingsRepository,
    private val tilordnetRessursService: TilordnetRessursService,
) {
    private val årsakerSomIkkeHarRevurderingsinformasjon =
        setOf(
            BehandlingÅrsak.G_OMREGNING,
            BehandlingÅrsak.SATSENDRING,
            BehandlingÅrsak.MIGRERING,
            BehandlingÅrsak.SANKSJON_1_MND,
        )

    fun validerHarGyldigRevurderingsinformasjon(saksbehandling: Saksbehandling) {
        if (
            saksbehandling.type != BehandlingType.FØRSTEGANGSBEHANDLING &&
            !årsakerSomIkkeHarRevurderingsinformasjon.contains(saksbehandling.årsak)
        ) {
            val revurderingsinformasjon = hentRevurderingsinformasjon(saksbehandling.id)
            brukerfeilHvis(revurderingsinformasjon.kravMottatt == null || revurderingsinformasjon.årsakRevurdering == null) {
                "Behandlingen mangler årsak til revurdering. fyll inn informasjonen i fanen for årsak revurdering"
            }
        }
    }

    fun hentÅrsakRevurdering(behandlingId: UUID): ÅrsakRevurdering? = årsakRevurderingsRepository.findByIdOrNull(behandlingId)

    fun hentRevurderingsinformasjon(behandlingId: UUID): RevurderingsinformasjonDto {
        val kravMottatt = behandlingService.hentSaksbehandling(behandlingId).kravMottatt
        val årsakRevurdering = årsakRevurderingsRepository.findByIdOrNull(behandlingId)
        return RevurderingsinformasjonDto(
            kravMottatt = kravMottatt,
            årsakRevurdering = årsakRevurdering?.tilDto(),
            endretTid = årsakRevurdering?.sporbar?.endret?.endretTid,
        )
    }

    @Transactional
    fun oppdaterRevurderingsinformasjon(
        saksbehandling: Saksbehandling,
        kravMottatt: LocalDate,
        årsakRevurdering: ÅrsakRevurderingDto,
    ) {
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og kan ikke oppdatere revurderingsinformasjon"
        }
        feilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler((saksbehandling.id))) {
            "Behandlingen har en ny eier og du kan derfor ikke oppdatere revurderingsinformasjon"
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
