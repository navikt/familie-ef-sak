package no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.amelding.inntektv2.InntektTypeV2
import no.nav.familie.ef.sak.amelding.inntektv2.InntektV2Response
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import kotlin.test.assertEquals

class InntektServiceTest {
    private val fagsakService: FagsakService = mockk(relaxed = true)

    @Nested
    inner class HentInntekt {

        @BeforeEach
        internal fun setUp() {
            every { fagsakService.hentAktivIdent(any()) } returns personIdent
        }

        @Test
        internal fun `parser inntektv2 response med riktig data struktur`() {
            val inntektV2ResponseJson: String = lesRessurs("json/inntektv2/InntektV2Response.json")
            val inntektV2Response = objectMapper.readValue<InntektV2Response>(inntektV2ResponseJson)

            val forventetMåned = YearMonth.of(2020, 3)
            val forventetInntektType: InntektTypeV2 = InntektTypeV2.LØNNSINNTEKT

            assertEquals(forventetMåned, inntektV2Response.maanedsData[0].maaned)
            assertEquals(forventetInntektType, inntektV2Response.maanedsData[0].inntektListe[0].type)
        }
    }

    fun lesRessurs(name: String): String {
        val resource = this::class.java.classLoader.getResource(name)
            ?: throw IllegalArgumentException("Resource not found: $name")
        return resource.readText(StandardCharsets.UTF_8)
    }

    companion object {
        const val personIdent = "10108000398"
    }
}