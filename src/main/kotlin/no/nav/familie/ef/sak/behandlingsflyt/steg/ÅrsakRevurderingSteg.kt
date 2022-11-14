package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.dto.tilDomene
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
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

        årsakRevurderingsRepository.deleteById(saksbehandling.id)
        årsakRevurderingsRepository.insert(data.tilDomene(saksbehandling.id))
    }
}
