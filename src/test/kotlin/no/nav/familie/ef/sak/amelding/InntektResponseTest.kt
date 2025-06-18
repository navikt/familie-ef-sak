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
        val inntektUtenOvergangsstønad = inntektResponse.førsteMånedMed10ProsentInntektsøkning(vedtak)
        assertThat(inntektUtenOvergangsstønad).isEqualTo(YearMonth.now().minusMonths(1))
    }

    @Test
    fun `finn førsteMånedMed10ProsentInntektsøkning - ignorer månedsinntekt tilsvarende årsinntekt på en halv g`() {
        val inntektV2ResponseJson: String = lesRessurs("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        inntektV2ResponseJson.replace("2025-04", YearMonth.now().minusMonths(2).toString())
        inntektV2ResponseJson.replace("2025-05", YearMonth.now().minusMonths(1).toString())
        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJson)

        val vedtak = vedtak(InntektWrapper(listOf(inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(6), YearMonth.now().plusMonths(1)), BigDecimal.valueOf(0)))))
        val inntektUtenOvergangsstønad = inntektResponse.førsteMånedMed10ProsentInntektsøkning(vedtak)
        assertThat(inntektUtenOvergangsstønad).isEqualTo(YearMonth.now().minusMonths(1))
    }

    @Test
    fun `TODO`() {
        val inntektV2ResponseJson: String = lesRessurs("json/inntekt/InntektLønnsinntektMedOvergangsstønadOgSykepenger.json")
        val inntektV2ResponseJsonModifisert = inntektV2ResponseJson.replace("57500.0", "100").replace("2025-05", YearMonth.now().minusMonths(1).toString()).replace("2025-04", YearMonth.now().minusMonths(2).toString())
        val inntektV2ResponseJsonModifisertForHøyInntekt = inntektV2ResponseJson.replace("57500.0", "99999").replace("2025-05", YearMonth.now().minusMonths(1).toString()).replace("2025-04", YearMonth.now().minusMonths(2).toString())

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisert)
        val inntektResponseForHøyInntekt = objectMapper.readValue<InntektResponse>(inntektV2ResponseJsonModifisertForHøyInntekt)

        val lavInntekt = inntektResponse.harInntektOverSisteTreMåneder
        val forHøyInntekt = inntektResponseForHøyInntekt.harInntektOverSisteTreMåneder

        assertThat(lavInntekt).isFalse()
        assertThat(forHøyInntekt).isTrue()
    }

    fun lesRessurs(name: String): String {
        val resource =
            this::class.java.classLoader.getResource(name)
                ?: throw IllegalArgumentException("Resource not found: $name")
        return resource.readText(StandardCharsets.UTF_8)
    }
}
