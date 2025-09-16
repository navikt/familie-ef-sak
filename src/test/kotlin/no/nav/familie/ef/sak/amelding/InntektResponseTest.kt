package no.nav.familie.ef.sak.no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.behandling.revurdering.BehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.PayloadBehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.lagInntektResponseFraMånedsinntekter
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.testutil.JsonFilUtil.Companion.lesFil
import no.nav.familie.ef.sak.testutil.JsonFilUtil.Companion.oppdaterMåneder
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class InntektResponseTest {
    @Test
    fun `finn førsteMånedMed10ProsentInntektsøkning`() {
        val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson, 2)
        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)

        val vedtak = vedtak(InntektWrapper(listOf(inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(6), YearMonth.now().plusMonths(1)), BigDecimal.valueOf(57500)))))
        val exception =
            assertThrows<IllegalStateException> {
                inntektResponse.førsteMånedMed10ProsentInntektsøkning(vedtak)
            }
        assertThat(exception.message).isEqualTo("Burde funnet måned med 10% inntekt for behandling: ${vedtak.behandlingId}")
    }

    @Test
    fun `finn førsteMånedMed10ProsentInntektsøkning - ignorer månedsinntekt tilsvarende årsinntekt på en halv g`() {
        val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson, 2)
        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)

        val vedtak = vedtak(InntektWrapper(listOf(inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(6), YearMonth.now().plusMonths(1)), BigDecimal.valueOf(0)))))
        val inntektUtenOvergangsstønad = inntektResponse.førsteMånedMed10ProsentInntektsøkning(vedtak)
        assertThat(inntektUtenOvergangsstønad).isEqualTo(YearMonth.now().minusMonths(1))
    }

    @Test
    fun `returner false hvis det finnes en måned med total inntekt som gir årsinntekt som er høyere enn fem og en halv G`() {
        val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson, 2).replace("57500.0", "100")
        val inntektV2ResponseJsonModifisertForHøyInntekt = oppdaterMåneder(inntektV2ResponseJson, 2).replace("57500.0", "99999")
        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        val inntektResponseForHøyInntekt = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisertForHøyInntekt)

        val lavInntekt = inntektResponse.finnesHøyMånedsinntektSomIkkeGirOvergangsstønad
        val forHøyInntekt = inntektResponseForHøyInntekt.finnesHøyMånedsinntektSomIkkeGirOvergangsstønad

        assertThat(lavInntekt).isFalse()
        assertThat(forHøyInntekt).isTrue()
    }

    @Test
    fun `Sjekk for differanse mellom inneværendemåneds inntekt og tre forrige måneder for differanse og gi true ved lav differanse og false hvis høy differanse`() {
        val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektKunFastLønnOgOvergangsstønad.json")
        val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson, 6, true)

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        assertThat(inntektResponse.ikkeForStortAvvikMellomInneværendemånedOgForrigeMånedBeløp()).isTrue

        val inntektV2ResponseJsonModifisertHøy = inntektV2ResponseJsonModifisert.replace("13000", "20000")
        val inntektResponseHøy = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisertHøy)
        assertThat(inntektResponseHøy.ikkeForStortAvvikMellomInneværendemånedOgForrigeMånedBeløp()).isFalse

        val inntektV2ResponseJsonModifisertLav = inntektV2ResponseJsonModifisert.replace("13000", "5000")
        val inntektResponseLav = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisertLav)
        assertThat(inntektResponseLav.ikkeForStortAvvikMellomInneværendemånedOgForrigeMånedBeløp()).isFalse
    }

    @Test
    fun `Sjekk at dersom det ikke er andre nav ytelser enn overgangstønad fra ef gi true og hvis det er andre ytelser false`() {
        val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektKunFastLønnOgOvergangsstønad.json")
        val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson, 6, true)

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        assertThat(inntektResponse.harIkkeAndreNavYtelser(YearMonth.now().minusMonths(3))).isTrue

        val inntektV2ResponseJsonMedAnnenYtelse: String = lesFil("json/inntekt/InntektFastlønnMedEnMånedMedKunFeriepenger.json")
        val inntektV2ResponseJsonModifisertMedAnnenYtelse = oppdaterMåneder(inntektV2ResponseJsonMedAnnenYtelse, 6, true)

        val inntektResponseMedAnnenYtelse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisertMedAnnenYtelse)
        assertThat(inntektResponseMedAnnenYtelse.harIkkeAndreNavYtelser(YearMonth.now().minusMonths(3))).isFalse
    }

    @Test
    fun `Sjekk at hvis kun arbeidsgiver harKunEnArbeidsgiver gir true og false hvis flere`() {
        val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektFastlønnToArbeidsgivere.json")
        val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson, 6, true)

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        assertThat(inntektResponse.harKunEnArbeidsgiver(YearMonth.now().minusMonths(3))).isFalse

        val inntektV2ResponseJsonMedAnnenYtelse: String = lesFil("json/inntekt/InntektKunFastLønnOgOvergangsstønad.json")
        val inntektV2ResponseJsonModifisertMedAnnenYtelse = oppdaterMåneder(inntektV2ResponseJsonMedAnnenYtelse, 6, true)

        val inntektResponseMedAnnenYtelse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisertMedAnnenYtelse)
        assertThat(inntektResponseMedAnnenYtelse.harKunEnArbeidsgiver(YearMonth.now().minusMonths(3))).isTrue
    }

    @Nested
    inner class ForventetInntekt {
        @Test
        fun `beregn forventet inntekt med feriepenger - vanlig case med fastlønn hvor feriepenger og trekk i lønn for ferie skal ignoreres`() {
            val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektFulltÅrMedFeriepenger.json")
            val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson)

            val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
            val månedsperiode = Månedsperiode(YearMonth.now().minusMonths(10), YearMonth.now().plusMonths(10))
            assertThat(inntektResponse.forventetMånedsinntekt(vedtak(UUID.randomUUID(), månedsperiode))).isEqualTo(10000)
            assertThat(inntektResponse.harMånedMedBareFeriepenger(YearMonth.now().minusMonths(3))).isFalse
        }

        @Test
        fun `beregn forventet inntekt - ikke ta med måned hvor det er kun feriepenger registrert`() {
            val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektFastlønnMedEnMånedMedKunFeriepenger.json")
            val inntektV2ResponseJsonModifisert = oppdaterMåneder(inntektV2ResponseJson)

            val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
            assertThat(inntektResponse.harMånedMedBareFeriepenger(YearMonth.now().minusMonths(3))).isTrue
        }

        @Test
        fun `beregn forventet inntekt hvor inntekt har økt siste 3 måneder - ikke ta med første måned med økning i beregning`() {
            val innmeldtMånedsinntekt = listOf(20_000, 28_000, 29_000, 30_000)
            val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)

            val vedtak = vedtak(InntektWrapper(listOf(inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(6), YearMonth.now().plusMonths(1)), BigDecimal.valueOf(20_000)))))

            val forventetInntekt = inntektResponse.forventetMånedsinntekt(vedtak)
            assertThat(forventetInntekt).isEqualTo(29_500)
        }
    }
}
