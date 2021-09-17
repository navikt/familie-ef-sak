package no.nav.familie.ef.sak.infrastruktur.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ef.sak.vedtak.dto.VedtakDtoModule

class ObjectMapperProvider {
    companion object {
        val objectMapper: ObjectMapper = no.nav.familie.kontrakter.felles.objectMapper.registerModule(VedtakDtoModule())
    }
}
