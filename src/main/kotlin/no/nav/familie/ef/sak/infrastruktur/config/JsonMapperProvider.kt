package no.nav.familie.ef.sak.infrastruktur.config

import no.nav.familie.ef.sak.vedtak.dto.VedtakDtoModule
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.io.InputStream
import java.net.URL
import no.nav.familie.kontrakter.felles.jsonMapper as kontraktJsonMapper

object JsonMapperProvider {
    val jsonMapper: JsonMapper =
        kontraktJsonMapper
            .rebuild()
            .addModule(VedtakDtoModule())
            .build()
}

/**
 * Extension functions to provide reified type parameter for readValue in Jackson 3
 */
inline fun <reified T> JsonMapper.readValue(content: String): T = readValue(content, object : TypeReference<T>() {})

inline fun <reified T> JsonMapper.readValue(content: ByteArray): T = readValue(content, object : TypeReference<T>() {})

inline fun <reified T> JsonMapper.readValue(content: URL): T = readValue(content.openStream(), object : TypeReference<T>() {})

inline fun <reified T> JsonMapper.readValue(content: File): T = readValue(content, object : TypeReference<T>() {})

inline fun <reified T> JsonMapper.readValue(content: InputStream): T = readValue(content, object : TypeReference<T>() {})
