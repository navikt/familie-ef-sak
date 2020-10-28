package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.Endret
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.repository.domain.søknad.Dokumentasjon
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import java.sql.Date
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource


@Configuration
@EnableJdbcAuditing
class DatabaseConfiguration : AbstractJdbcConfiguration() {

    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> {
        return AuditorAware {
            Optional.of(Endret())
        }
    }

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(listOf(UtbetalingsoppdragTilStringConverter(),
                                            StringTilUtbetalingsoppdragConverter(),
                                            DokumentTilStringConverter(),
                                            StringTilDokumentConverter(),
                                            YearMonthTilLocalDateConverter(),
                                            LocalDateTilYearMonthConverter(),
                                            TilkjentYtelseStatusTilStringConverter(),
                                            StringTilDelvilkårConverter(),
                                            DelvilkårTilStringConverter()
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
    class DokumentTilStringConverter : Converter<Dokumentasjon, String> {

        override fun convert(dokumentasjon: Dokumentasjon?): String? {
            return objectMapper.writeValueAsString(dokumentasjon)
        }
    }

    @ReadingConverter
    class StringTilDokumentConverter : Converter<String, Dokumentasjon> {

        override fun convert(s: String?): Dokumentasjon? {
            return objectMapper.readValue(s, Dokumentasjon::class.java)
        }
    }

    @WritingConverter
    class YearMonthTilLocalDateConverter : Converter<YearMonth, LocalDate> {

        override fun convert(yearMonth: YearMonth?): LocalDate? {
            return yearMonth?.let {
                LocalDate.of(it.year, it.month, 1)
            }
        }
    }

    @ReadingConverter
    class LocalDateTilYearMonthConverter : Converter<Date, YearMonth> {

        override fun convert(date: Date?): YearMonth? {
            return date?.let {
                val localDate = date.toLocalDate()
                YearMonth.of(localDate.year, localDate.month)
            }
        }
    }


    @WritingConverter
    class TilkjentYtelseStatusTilStringConverter : Converter<TilkjentYtelseStatus, String> {

        override fun convert(tilkjentYtelseStatus: TilkjentYtelseStatus) = tilkjentYtelseStatus.name
    }

    @ReadingConverter
    class StringTilDelvilkårConverter : Converter<ByteArray, DelvilkårsvurderingWrapper> {

        override fun convert(byteArray: ByteArray): DelvilkårsvurderingWrapper {
            return DelvilkårsvurderingWrapper(objectMapper.readValue(byteArray))
        }
    }

    @WritingConverter
    class DelvilkårTilStringConverter : Converter<DelvilkårsvurderingWrapper, ByteArray> {

        override fun convert(delvilkårsvurdering: DelvilkårsvurderingWrapper) = objectMapper.writeValueAsBytes(delvilkårsvurdering.delvilkårsvurderinger)
    }

}
