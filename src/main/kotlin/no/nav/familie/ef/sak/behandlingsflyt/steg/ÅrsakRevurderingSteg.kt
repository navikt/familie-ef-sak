package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingService
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import org.springframework.stereotype.Service

@Service
class ÅrsakRevurderingSteg(
    private val årsakRevurderingService: ÅrsakRevurderingService,
    private val tilordnetRessursService: TilordnetRessursService,
) : BehandlingSteg<RevurderingsinformasjonDto> {
    override fun stegType(): StegType = StegType.REVURDERING_ÅRSAK

    override fun utførOgReturnerNesteSteg(
        saksbehandling: Saksbehandling,
        data: RevurderingsinformasjonDto,
    ): StegType {
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
        årsakRevurdering: ÅrsakRevurderingDto,
    ) {
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og kan ikke oppdatere årsak til revurdering"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(saksbehandling.id)) {
            "Behandlingen har en ny eier og du kan derfor ikke oppdatere årsak til revurdering"
        }
        brukerfeilHvisIkke(årsakRevurdering.årsak.erGyldigForStønadstype(saksbehandling.stønadstype)) {
            "Årsak er ikke gyldig for stønadstype"
        }
        brukerfeilHvis(årsakRevurdering.årsak == Revurderingsårsak.ANNET && årsakRevurdering.beskrivelse.isNullOrBlank()) {
            "Må ha med beskrivelse når årsak er annet"
        }
    }

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: RevurderingsinformasjonDto,
    ) {
        error("utførOgReturnerNesteSteg utfør og returnerer steg")
    }
}
