package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Revurderingsårsak
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingService
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import org.springframework.stereotype.Service

@Service
class ÅrsakRevurderingSteg(
    private val årsakRevurderingService: ÅrsakRevurderingService
) : BehandlingSteg<RevurderingsinformasjonDto> {

    override fun stegType(): StegType {
        return StegType.REVURDERING_ÅRSAK
    }

    override fun utførOgReturnerNesteSteg(saksbehandling: Saksbehandling, data: RevurderingsinformasjonDto): StegType {
        val (kravMottatt, årsakRevurdering) = data

        feilHvis(kravMottatt == null) {
            "Mangler kravMottatt"
        }
        feilHvis(årsakRevurdering == null) {
            "Mangler årsakRevurdering"
        }

        validerGyldigeVerdier(saksbehandling, årsakRevurdering)

        årsakRevurderingService.oppdaterRevurderingsinformasjon(saksbehandling, kravMottatt, årsakRevurdering)

        // returnerer behandlingen sitt nåværende steg for å ikke endre steg
        return saksbehandling.steg
    }

    private fun validerGyldigeVerdier(
        saksbehandling: Saksbehandling,
        årsakRevurdering: ÅrsakRevurderingDto
    ) {
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
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: RevurderingsinformasjonDto) {
        error("utførOgReturnerNesteSteg utfør og returnerer steg")
    }
}
