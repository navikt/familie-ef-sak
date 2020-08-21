package no.nav.familie.ef.sak.config

import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource


@Configuration
class DatabaseConfiguration : AbstractJdbcConfiguration() {

    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(listOf(
                UtbetalingsoppdragTilStringConverter(),
                StringTilUtbetalingsoppdragConverter(),
                TilkjentYtelseStatusTilStringConverter()
        ))
    }

    @WritingConverter
    class UtbetalingsoppdragTilStringConverter : Converter<Utbetalingsoppdrag, String> {

        override fun convert(utbetalingsoppdrag: Utbetalingsoppdrag): String {
            return objectMapper.writeValueAsString(utbetalingsoppdrag)
        }
    }

    @ReadingConverter
    class StringTilUtbetalingsoppdragConverter : Converter<String, Utbetalingsoppdrag> {

        override fun convert(string: String): Utbetalingsoppdrag {
            return objectMapper.readValue(string, Utbetalingsoppdrag::class.java)
        }
    }

    @WritingConverter
    class TilkjentYtelseStatusTilStringConverter : Converter<TilkjentYtelseStatus, String> {

        override fun convert(tilkjentYtelseStatus: TilkjentYtelseStatus) = tilkjentYtelseStatus.name
    }

}
