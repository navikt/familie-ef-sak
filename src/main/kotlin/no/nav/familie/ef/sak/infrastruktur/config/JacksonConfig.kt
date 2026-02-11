package no.nav.familie.ef.sak.infrastruktur.config

import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class JacksonConfig : WebMvcConfigurer {
    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.removeIf { it is MappingJackson2HttpMessageConverter }

        // Sørg for at ByteArrayHttpMessageConverter er først (kreves for springdoc)
        converters.removeIf { it is ByteArrayHttpMessageConverter }
        converters.add(0, ByteArrayHttpMessageConverter())

        // Legg til custom Jackson-converter etter ByteArrayHttpMessageConverter
        converters.add(1, MappingJackson2HttpMessageConverter(objectMapper))
    }
}
