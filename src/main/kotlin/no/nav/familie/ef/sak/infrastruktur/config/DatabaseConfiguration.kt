package no.nav.familie.ef.sak.infrastruktur.config

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.brev.domain.OrganisasjonerWrapper
import no.nav.familie.ef.sak.brev.domain.PersonerWrapper
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.felles.domain.JsonWrapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidssituasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.GjelderDeg
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerWrapper
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import org.apache.commons.lang3.StringUtils
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
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
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations = NamedParameterJdbcTemplate(dataSource)

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> =
        AuditorAware {
            Optional.of(Endret())
        }

    @Bean
    fun verifyIgnoreIfProd(
        @Value("\${spring.flyway.placeholders.ignoreIfProd}") ignoreIfProd: String,
        environment: Environment,
    ): FlywayConfigurationCustomizer {
        val isProd = environment.activeProfiles.contains("prod")
        val ignore = ignoreIfProd == "--"
        return FlywayConfigurationCustomizer {
            if (isProd && !ignore) {
                throw RuntimeException("Prod profile men har ikke riktig verdi for placeholder ignoreIfProd=$ignoreIfProd")
            }
            if (!isProd && ignore) {
                throw RuntimeException("Profile=${environment.activeProfiles} men har ignoreIfProd=--")
            }
        }
    }

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions =
        JdbcCustomConversions(
            listOf(
                UtbetalingsoppdragTilStringConverter(),
                StringTilUtbetalingsoppdragConverter(),
                DokumentTilStringConverter(),
                StringTilDokumentConverter(),
                YearMonthTilLocalDateConverter(),
                LocalDateTilYearMonthConverter(),
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
                FilTilBytearrayConverter(),
                BytearrayTilFilConverter(),
                PGobjectTilVedtaksperioder(),
                VedtaksperiodeTilPGobjectConverter(),
                PGobjectTilInntektsperiode(),
                GrunnlagsdataTilPGobjectConverter(),
                PGobjectTilGrunnlagsdata(),
                InntektsperiodeTilPGobjectConverter(),
                PGobjectTilTilleggsstønadConverter(),
                TilleggsstønadTilPGobjectConverter(),
                PGobjectTilKontantstøtteConverter(),
                KontantstøtteTilPGobjectConverter(),
                PGobjectTilBarnetilsynConverter(),
                BarnetilsynTilPGobjectConverter(),
                PGobjectTilDetaljertSimuleringResultat(),
                DetaljertSimuleringResultatTilPGobjectConverter(),
                PGobjectTilBeriketSimuleringsresultat(),
                BeriketSimuleringsresultatTilPGobjectConverter(),
                PGobjectTilBrevmottakerPersoner(),
                BrevmottakerePersonerTilPGobjectConverter(),
                PGobjectTilBrevmottakerOrganisasjoner(),
                BrevmottakereOrganisasjonerTilPGobjectConverter(),
                PGobjectTilSkolepengerConverter(),
                SkolepengerTilPGobjectConverter(),
            ),
        )

    @WritingConverter
    class UtbetalingsoppdragTilStringConverter : Converter<Utbetalingsoppdrag, String> {
        override fun convert(utbetalingsoppdrag: Utbetalingsoppdrag): String = objectMapper.writeValueAsString(utbetalingsoppdrag)
    }

    @ReadingConverter
    class StringTilUtbetalingsoppdragConverter : Converter<String, Utbetalingsoppdrag> {
        override fun convert(string: String): Utbetalingsoppdrag = objectMapper.readValue(string, Utbetalingsoppdrag::class.java)
    }

    @WritingConverter
    class DokumentTilStringConverter : Converter<Dokumentasjon, String> {
        override fun convert(dokumentasjon: Dokumentasjon): String? = objectMapper.writeValueAsString(dokumentasjon)
    }

    @ReadingConverter
    class StringTilDokumentConverter : Converter<String, Dokumentasjon> {
        override fun convert(s: String): Dokumentasjon = objectMapper.readValue(s, Dokumentasjon::class.java)
    }

    @WritingConverter
    class YearMonthTilLocalDateConverter : Converter<YearMonth, LocalDate> {
        override fun convert(yearMonth: YearMonth): LocalDate = yearMonth.atDay(1)
    }

    @ReadingConverter
    class LocalDateTilYearMonthConverter : Converter<Date, YearMonth> {
        override fun convert(date: Date): YearMonth = YearMonth.from(date.toLocalDate())
    }

    @ReadingConverter
    class PGobjectTilDelvilkårConverter : Converter<PGobject, DelvilkårsvurderingWrapper> {
        override fun convert(pGobject: PGobject): DelvilkårsvurderingWrapper = DelvilkårsvurderingWrapper(pGobject.value?.let { objectMapper.readValue(it) } ?: emptyList())
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
        override fun convert(verdier: GjelderDeg): String = StringUtils.join(verdier.verdier, ";")
    }

    @ReadingConverter
    class StringTilGjelderDegConverter : Converter<String, GjelderDeg> {
        override fun convert(verdi: String): GjelderDeg = GjelderDeg(verdi.split(";"))
    }

    @WritingConverter
    class ArbeidssituasjonTilStringConverter : Converter<Arbeidssituasjon, String> {
        override fun convert(verdier: Arbeidssituasjon): String = StringUtils.join(verdier.verdier, ";")
    }

    @ReadingConverter
    class StringTilArbeidssituasjonConverter : Converter<String, Arbeidssituasjon> {
        override fun convert(verdi: String): Arbeidssituasjon = Arbeidssituasjon(verdi.split(";"))
    }

    @ReadingConverter
    class PGobjectTilJsonWrapperConverter : Converter<PGobject, JsonWrapper?> {
        override fun convert(pGobject: PGobject): JsonWrapper? = pGobject.value?.let { JsonWrapper(it) }
    }

    @WritingConverter
    class JsonWrapperTilPGobjectConverter : Converter<JsonWrapper, PGobject> {
        override fun convert(jsonWrapper: JsonWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonWrapper.json
            }
    }

    @WritingConverter
    class GrunnlagsdataTilPGobjectConverter : Converter<GrunnlagsdataDomene, PGobject> {
        override fun convert(data: GrunnlagsdataDomene): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data)
            }
    }

    @ReadingConverter
    class PGobjectTilGrunnlagsdata : Converter<PGobject, GrunnlagsdataDomene> {
        override fun convert(pGobject: PGobject): GrunnlagsdataDomene = objectMapper.readValue(pGobject.value!!)
    }

    @WritingConverter
    class FilTilBytearrayConverter : Converter<Fil, ByteArray> {
        override fun convert(fil: Fil): ByteArray = fil.bytes
    }

    @ReadingConverter
    class BytearrayTilFilConverter : Converter<ByteArray, Fil> {
        override fun convert(bytes: ByteArray): Fil = Fil(bytes)
    }

    @ReadingConverter
    class PGobjectTilVedtaksperioder : Converter<PGobject, PeriodeWrapper> {
        override fun convert(pGobject: PGobject): PeriodeWrapper = PeriodeWrapper(pGobject.value?.let { objectMapper.readValue(it) } ?: emptyList())
    }

    @WritingConverter
    class VedtaksperiodeTilPGobjectConverter : Converter<PeriodeWrapper, PGobject> {
        override fun convert(perioder: PeriodeWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(perioder.perioder)
            }
    }

    @ReadingConverter
    class PGobjectTilInntektsperiode : Converter<PGobject, InntektWrapper> {
        override fun convert(pGobject: PGobject): InntektWrapper = InntektWrapper(pGobject.value?.let { objectMapper.readValue(it) } ?: emptyList())
    }

    @WritingConverter
    class InntektsperiodeTilPGobjectConverter : Converter<InntektWrapper, PGobject> {
        override fun convert(inntekter: InntektWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(inntekter.inntekter)
            }
    }

    @ReadingConverter
    class PGobjectTilBarnetilsynConverter : Converter<PGobject, BarnetilsynWrapper> {
        override fun convert(pGobject: PGobject): BarnetilsynWrapper {
            val barnetilsynVerdi: BarnetilsynWrapper? = pGobject.value?.let { objectMapper.readValue(it) }
            return barnetilsynVerdi ?: BarnetilsynWrapper(perioder = emptyList(), begrunnelse = null)
        }
    }

    @WritingConverter
    class BarnetilsynTilPGobjectConverter : Converter<BarnetilsynWrapper, PGobject> {
        override fun convert(barnetilsyn: BarnetilsynWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(barnetilsyn)
            }
    }

    @ReadingConverter
    class PGobjectTilSkolepengerConverter : Converter<PGobject, SkolepengerWrapper> {
        override fun convert(pGobject: PGobject): SkolepengerWrapper {
            val eksisterendeVerdi: SkolepengerWrapper? = pGobject.value?.let { objectMapper.readValue(it) }
            return eksisterendeVerdi ?: SkolepengerWrapper(skoleårsperioder = emptyList(), begrunnelse = null)
        }
    }

    @WritingConverter
    class SkolepengerTilPGobjectConverter : Converter<SkolepengerWrapper, PGobject> {
        override fun convert(barnetilsyn: SkolepengerWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(barnetilsyn)
            }
    }

    @ReadingConverter
    class PGobjectTilKontantstøtteConverter : Converter<PGobject, KontantstøtteWrapper> {
        override fun convert(pGobject: PGobject): KontantstøtteWrapper {
            val kontantstøtteVerdi: KontantstøtteWrapper? = pGobject.value?.let { objectMapper.readValue(it) }
            return kontantstøtteVerdi ?: KontantstøtteWrapper(perioder = emptyList())
        }
    }

    @WritingConverter
    class KontantstøtteTilPGobjectConverter : Converter<KontantstøtteWrapper, PGobject> {
        override fun convert(kontantstøtte: KontantstøtteWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(kontantstøtte)
            }
    }

    @ReadingConverter
    class PGobjectTilTilleggsstønadConverter : Converter<PGobject, TilleggsstønadWrapper> {
        override fun convert(pGobject: PGobject): TilleggsstønadWrapper {
            val tilleggstønadVerdi: TilleggsstønadWrapper? = pGobject.value?.let { objectMapper.readValue(it) }
            return tilleggstønadVerdi ?: TilleggsstønadWrapper(
                perioder = emptyList(),
                begrunnelse = null,
            )
        }
    }

    @WritingConverter
    class TilleggsstønadTilPGobjectConverter : Converter<TilleggsstønadWrapper, PGobject> {
        override fun convert(tilleggsstønad: TilleggsstønadWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(tilleggsstønad)
            }
    }

    @ReadingConverter
    class PGobjectTilDetaljertSimuleringResultat : Converter<PGobject, DetaljertSimuleringResultat> {
        override fun convert(pGobject: PGobject): DetaljertSimuleringResultat = DetaljertSimuleringResultat(pGobject.value?.let { objectMapper.readValue(it) } ?: emptyList())
    }

    @WritingConverter
    class DetaljertSimuleringResultatTilPGobjectConverter : Converter<DetaljertSimuleringResultat, PGobject> {
        override fun convert(simuleringsresultat: DetaljertSimuleringResultat): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(simuleringsresultat.simuleringMottaker)
            }
    }

    @ReadingConverter
    class PGobjectTilBeriketSimuleringsresultat : Converter<PGobject, BeriketSimuleringsresultat?> {
        override fun convert(pGobject: PGobject): BeriketSimuleringsresultat? = pGobject.value?.let { objectMapper.readValue(it) }
    }

    @WritingConverter
    class BeriketSimuleringsresultatTilPGobjectConverter : Converter<BeriketSimuleringsresultat, PGobject> {
        override fun convert(simuleringsresultat: BeriketSimuleringsresultat): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(simuleringsresultat)
            }
    }

    @ReadingConverter
    class PGobjectTilBrevmottakerPersoner : Converter<PGobject, PersonerWrapper> {
        override fun convert(pGobject: PGobject): PersonerWrapper? = pGobject.value?.let { objectMapper.readValue(it) }
    }

    @WritingConverter
    class BrevmottakerePersonerTilPGobjectConverter : Converter<PersonerWrapper, PGobject> {
        override fun convert(mottakere: PersonerWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(mottakere)
            }
    }

    @ReadingConverter
    class PGobjectTilBrevmottakerOrganisasjoner : Converter<PGobject, OrganisasjonerWrapper> {
        override fun convert(pGobject: PGobject): OrganisasjonerWrapper? = pGobject.value?.let { objectMapper.readValue(it) }
    }

    @WritingConverter
    class BrevmottakereOrganisasjonerTilPGobjectConverter : Converter<OrganisasjonerWrapper, PGobject> {
        override fun convert(mottakere: OrganisasjonerWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(mottakere)
            }
    }
}
