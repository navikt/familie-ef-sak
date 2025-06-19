package no.nav.familie.ef.sak.no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class InntektResponseTest {
    @Test
    fun `finn førsteMånedMed10ProsentInntektsøkning`() {
        val inntektV2ResponseJson: String = lesRessurs("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        val inntektV2ResponseJsonModifisert = inntektV2ResponseJson.replace("2025-05", YearMonth.now().minusMonths(1).toString()).replace("2025-04", YearMonth.now().minusMonths(2).toString())
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
        val inntektV2ResponseJson: String = lesRessurs("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        val inntektV2ResponseJsonModifisert = inntektV2ResponseJson.replace("2025-05", YearMonth.now().minusMonths(1).toString()).replace("2025-04", YearMonth.now().minusMonths(2).toString())
        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)

        val vedtak = vedtak(InntektWrapper(listOf(inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(6), YearMonth.now().plusMonths(1)), BigDecimal.valueOf(0)))))
        val inntektUtenOvergangsstønad = inntektResponse.førsteMånedMed10ProsentInntektsøkning(vedtak)
        assertThat(inntektUtenOvergangsstønad).isEqualTo(YearMonth.now().minusMonths(1))
    }

    @Test
    fun `returner false hvis det finnes en måned med total inntekt som gir årsinntekt som er høyere enn fem og en halv G`() {
        val inntektV2ResponseJson: String = lesRessurs("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        val inntektV2ResponseJsonModifisert = inntektV2ResponseJson.replace("57500.0", "100").replace("2025-05", YearMonth.now().minusMonths(1).toString()).replace("2025-04", YearMonth.now().minusMonths(2).toString())
        val inntektV2ResponseJsonModifisertForHøyInntekt = inntektV2ResponseJson.replace("57500.0", "99999").replace("2025-05", YearMonth.now().minusMonths(1).toString()).replace("2025-04", YearMonth.now().minusMonths(2).toString())

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        val inntektResponseForHøyInntekt = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisertForHøyInntekt)

        val lavInntekt = inntektResponse.finnesHøyMånedsinntektSomIkkeGirOvergangsstønad
        val forHøyInntekt = inntektResponseForHøyInntekt.finnesHøyMånedsinntektSomIkkeGirOvergangsstønad

        assertThat(lavInntekt).isFalse()
        assertThat(forHøyInntekt).isTrue()
    }

    @Test
    fun `beregn forventet inntekt med feriepenger - vanlig case med fastlønn hvor feriepenger og trekk i lønn for ferie skal ignoreres`() {
        val inntektV2ResponseJson: String = lesRessurs("json/inntekt/InntektFulltÅrMedFeriepenger.json")
        val inntektV2ResponseJsonModifisert =
            inntektV2ResponseJson
                .replace("2020-05", YearMonth.now().minusMonths(3).toString())
                .replace("2020-06", YearMonth.now().minusMonths(2).toString())
                .replace("2020-07", YearMonth.now().minusMonths(1).toString())

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        assertThat(inntektResponse.forventetMånedsinntekt()).isEqualTo(10333)
    }

    @Test
    fun `beregn forventet inntekt - ikke ta med måned hvor det er kun feriepenger registrert`() {
        val inntektV2ResponseJson: String = lesRessurs("json/inntekt/InntektFastlønnMedEnMånedMedKunFeriepenger.json")
        val inntektV2ResponseJsonModifisert =
            inntektV2ResponseJson
                .replace("2020-04", YearMonth.now().minusMonths(4).toString())
                .replace("2020-05", YearMonth.now().minusMonths(3).toString())
                .replace("2020-06", YearMonth.now().minusMonths(2).toString()) // Feriepenger som ignoreres
                .replace("2020-07", YearMonth.now().minusMonths(1).toString())

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        assertThat(inntektResponse.forventetMånedsinntekt()).isEqualTo(11333)
    }

    fun lesRessurs(name: String): String {
        val resource =
            this::class.java.classLoader.getResource(name)
                ?: throw IllegalArgumentException("Resource not found: $name")
        return resource.readText(StandardCharsets.UTF_8)
    }
}
