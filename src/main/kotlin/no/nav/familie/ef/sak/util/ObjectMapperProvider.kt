package no.nav.familie.ef.sak.util

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ef.sak.beregning.VedtakDtoModule

class ObjectMapperProvider {
    companion object {
        val objectMapper: ObjectMapper = no.nav.familie.kontrakter.felles.objectMapper.registerModule(VedtakDtoModule())
    }
}
