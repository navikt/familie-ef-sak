package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import org.springframework.stereotype.Service

@Service
class RegistrereSøknadSteg(
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        // TODO: Søknad og behandling kan kobles sammen her
        return hentNesteSteg(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_SØKNAD
    }
}

