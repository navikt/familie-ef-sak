package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.stereotype.Component
import javax.sql.DataSource


@Component
class TestConfig {


    @Bean
    fun dataSource(): DataSource { // no need shutdown, EmbeddedDatabaseFactoryBean will take care of this
        val builder = EmbeddedDatabaseBuilder()
        return builder
                .setType(EmbeddedDatabaseType.H2) //.H2 or .DERBY
                .build()
    }

}
