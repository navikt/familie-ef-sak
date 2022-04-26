package no.nav.familie.ef.sak.infrastruktur.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ef.sak.vedtak.dto.VedtakDtoModule

object ObjectMapperProvider {

    val objectMapper: ObjectMapper = no.nav.familie.kontrakter.felles.objectMapper.registerModule(VedtakDtoModule())
}
