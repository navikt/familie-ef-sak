package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Revurderingsårsak
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.tilDomene
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import org.springframework.stereotype.Service

@Service
class ÅrsakRevurderingSteg(
    private val årsakRevurderingsRepository: ÅrsakRevurderingsRepository,
    private val behandlingService: BehandlingService
) : BehandlingSteg<RevurderingsinformasjonDto> {

    override fun stegType(): StegType {
        return StegType.REVURDERING_ÅRSAK
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: RevurderingsinformasjonDto) {
        val (kravMottatt, årsakRevurdering) = data

        feilHvis(kravMottatt == null) {
            "Mangler kravMottatt"
        }
        feilHvis(årsakRevurdering == null) {
            "Mangler årsakRevurdering"
        }

        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og kan ikke oppdatere årsak til revurdering"
        }
        brukerfeilHvisIkke(årsakRevurdering.årsak.erGyldigForStønadstype(saksbehandling.stønadstype)) {
            "Årsak er ikke gyldig for stønadstype"
        }
        brukerfeilHvis(årsakRevurdering.årsak == Revurderingsårsak.ANNET && årsakRevurdering.beskrivelse.isNullOrBlank()) {
            "Må ha med beskrivelse når årsak er annet"
        }

        brukerfeilHvis(årsakRevurdering.årsak != Revurderingsårsak.ANNET && årsakRevurdering.beskrivelse != null) {
            "Kan ikke ha med beskrivelse når årsak er noe annet en annet"
        }

        årsakRevurderingsRepository.deleteById(saksbehandling.id)
        årsakRevurderingsRepository.insert(årsakRevurdering.tilDomene(saksbehandling.id))
        behandlingService.oppdaterKravMottatt(saksbehandling.id, kravMottatt)
    }
}
