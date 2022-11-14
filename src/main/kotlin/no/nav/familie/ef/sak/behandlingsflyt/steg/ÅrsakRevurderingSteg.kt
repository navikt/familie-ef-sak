package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Revurderingsårsak
import no.nav.familie.ef.sak.behandling.dto.tilDomene
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ÅrsakRevurderingSteg(
    private val årsakRevurderingsRepository: ÅrsakRevurderingsRepository
) : BehandlingSteg<ÅrsakRevurderingDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun stegType(): StegType {
        return StegType.REVURDERING_ÅRSAK
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: ÅrsakRevurderingDto) {

        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og kan ikke oppdatere årsak til revurdering"
        }
        brukerfeilHvisIkke(data.årsak.erGyldigForStønadstype(saksbehandling.stønadstype)) {
            "Årsak er ikke gyldig for stønadstype"
        }
        brukerfeilHvis(data.årsak == Revurderingsårsak.ANNET && data.beskrivelse.isNullOrBlank()) {
            "Mangler beskrivelse"
        }

        årsakRevurderingsRepository.deleteById(saksbehandling.id)
        årsakRevurderingsRepository.insert(data.tilDomene(saksbehandling.id))
    }
}
