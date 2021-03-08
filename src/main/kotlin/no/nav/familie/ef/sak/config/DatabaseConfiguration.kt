package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.Endret
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.JsonWrapper
import no.nav.familie.ef.sak.repository.domain.RegistergrunnlagData
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.repository.domain.søknad.Arbeidssituasjon
import no.nav.familie.ef.sak.repository.domain.søknad.Dokumentasjon
import no.nav.familie.ef.sak.repository.domain.søknad.GjelderDeg
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import org.apache.commons.lang3.StringUtils
import org.postgresql.util.PGobject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import java.sql.Date
import java.time.LocalDate
import java.time.YearMonth
import java.util.Optional
import javax.sql.DataSource


@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories("no.nav.familie")
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
                                            PropertiesWrapperTilStringConverter(),
                                            StringTilPropertiesWrapperConverter(),
                                            PGobjectTilDelvilkårConverter(),
                                            DelvilkårTilPGobjectConverter(),
                                            GjelderDegTilStringConverter(),
                                            StringTilGjelderDegConverter(),
                                            ArbeidssituasjonTilStringConverter(),
                                            StringTilArbeidssituasjonConverter(),
                                            PGobjectTilJsonWrapperConverter(),
                                            JsonWrapperTilPGobjectConverter(),
                                            BrevRequestTilStringConverter(),
                                            StringTilBrevRequestConverter(),
                                            FilTilBytearrayConverter(),
                                            BytearrayTilFilConverter(),
                                            PGobjectTilGrunnlagsdataConverter(),
                                            GrunnlagsdataTilPGobjectConverter()
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
    class PGobjectTilDelvilkårConverter : Converter<PGobject, DelvilkårsvurderingWrapper> {

        override fun convert(pGobject: PGobject): DelvilkårsvurderingWrapper {
            return DelvilkårsvurderingWrapper(pGobject.value?.let { objectMapper.readValue(it) } ?: emptyList())
        }
    }

    @WritingConverter
    class DelvilkårTilPGobjectConverter : Converter<DelvilkårsvurderingWrapper, PGobject> {

        override fun convert(delvilkårsvurdering: DelvilkårsvurderingWrapper): PGobject =
                PGobject().apply {
                    type = "json"
                    value = objectMapper.writeValueAsString(delvilkårsvurdering.delvilkårsvurderinger)
                }
    }

    @WritingConverter
    class GjelderDegTilStringConverter : Converter<GjelderDeg, String> {

        override fun convert(verdier: GjelderDeg): String {
            return StringUtils.join(verdier.verdier, ";")
        }
    }

    @ReadingConverter
    class StringTilGjelderDegConverter : Converter<String, GjelderDeg> {

        override fun convert(verdi: String): GjelderDeg {
            return GjelderDeg(verdi.split(";"))

        }
    }

    @WritingConverter
    class BrevRequestTilStringConverter : Converter<BrevRequest, String> {

        override fun convert(brevRequest: BrevRequest): String {
            return objectMapper.writeValueAsString(brevRequest)
        }
    }

    @ReadingConverter
    class StringTilBrevRequestConverter : Converter<String, BrevRequest> {

        override fun convert(brevRequest: String): BrevRequest {
            return objectMapper.readValue(brevRequest, BrevRequest::class.java)
        }
    }


    @WritingConverter
    class ArbeidssituasjonTilStringConverter : Converter<Arbeidssituasjon, String> {

        override fun convert(verdier: Arbeidssituasjon): String {
            return StringUtils.join(verdier.verdier, ";")
        }
    }

    @ReadingConverter
    class StringTilArbeidssituasjonConverter : Converter<String, Arbeidssituasjon> {

        override fun convert(verdi: String): Arbeidssituasjon {
            return Arbeidssituasjon(verdi.split(";"))

        }
    }

    @ReadingConverter
    class PGobjectTilJsonWrapperConverter : Converter<PGobject, JsonWrapper?> {

        override fun convert(pGobject: PGobject): JsonWrapper? {
            return pGobject.value?.let { JsonWrapper(it) }
        }
    }

    @WritingConverter
    class JsonWrapperTilPGobjectConverter : Converter<JsonWrapper?, PGobject> {

        override fun convert(jsonWrapper: JsonWrapper?): PGobject =
                PGobject().apply {
                    type = "json"
                    value = jsonWrapper?.let { it.json }
                }
    }

    @ReadingConverter
    class PGobjectTilGrunnlagsdataConverter : Converter<PGobject, RegistergrunnlagData> {

        override fun convert(pGobject: PGobject): RegistergrunnlagData {
            return objectMapper.readValue(pGobject.value!!)
        }
    }

    @WritingConverter
    class GrunnlagsdataTilPGobjectConverter : Converter<RegistergrunnlagData, PGobject> {

        override fun convert(data: RegistergrunnlagData): PGobject =
                PGobject().apply {
                    type = "json"
                    value = objectMapper.writeValueAsString(data)
                }
    }

    @WritingConverter
    class FilTilBytearrayConverter : Converter<Fil, ByteArray> {

        override fun convert(fil: Fil): ByteArray {
            return fil.bytes
        }
    }

    @ReadingConverter
    class BytearrayTilFilConverter : Converter<ByteArray, Fil> {

        override fun convert(bytes: ByteArray): Fil {
            return Fil(bytes)
        }
    }

}
