package no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.amelding.inntektv2.InntektTypeV2
import no.nav.familie.ef.sak.amelding.inntektv2.InntektV2Response
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class InntektServiceTest {
    private val fagsakService: FagsakService = mockk(relaxed = true)
    private val aMeldingInntektClient: AMeldingInntektClient = mockk(relaxed = true)
    private val fagsakPersonService: FagsakPersonService = mockk(relaxed = true)
    private val inntektMapper: InntektMapper = mockk(relaxed = true)

    private val inntektService: InntektService =
        InntektService(
            aMeldingInntektClient = aMeldingInntektClient,
            fagsakService = fagsakService,
            fagsakPersonService = fagsakPersonService,
            inntektMapper = inntektMapper,
        )

    private val fagsakId = UUID.randomUUID()
    private val personIdent = "10108000398"

    @Nested
    inner class ParseInntektV2Reponse {
        @Test
        internal fun `parser generell inntektv2 response med riktig data struktur`() {
            val inntektV2ResponseJson: String = lesRessurs("json/inntektv2/GenerellInntektV2Reponse.json")
            val inntektV2Response = objectMapper.readValue<InntektV2Response>(inntektV2ResponseJson)

            val forventetMåned = YearMonth.of(2020, 3)
            val forventetInntektType: InntektTypeV2 = InntektTypeV2.LØNNSINNTEKT

            assertEquals(forventetMåned, inntektV2Response.maanedsData[0].måned)
            assertEquals(forventetInntektType, inntektV2Response.maanedsData[0].inntektListe[0].type)
        }

        @Test
        internal fun `parser inntektv2 response med forskjellige inntekt typer`() {
            val inntektV2ResponseJson: String = lesRessurs("json/inntektv2/FlereInntektTyperInntektV2Response.json")
            val inntektV2Response = objectMapper.readValue<InntektV2Response>(inntektV2ResponseJson)

            val forventeteInntektTyper =
                listOf(
                    InntektTypeV2.LØNNSINNTEKT,
                    InntektTypeV2.NAERINGSINNTEKT,
                    InntektTypeV2.YTELSE_FRA_OFFENTLIGE,
                    InntektTypeV2.PENSJON_ELLER_TRYGD,
                )

            val faktiskeInntektTyper =
                inntektV2Response.maanedsData
                    .flatMap { it.inntektListe }
                    .map { it.type }
                    .distinct()

            assertEquals(forventeteInntektTyper.sorted(), faktiskeInntektTyper.sorted())
        }
    }

    @Nested
    inner class HentInntekt {
        @BeforeEach
        internal fun setUp() {
            every { fagsakService.hentAktivIdent(any()) } returns personIdent
        }

        @Test
        internal fun `skal hente årsinntekt og summere riktig`() {
            val inntektV2ResponseJson: String =
                lesRessurs("json/inntektv2/År2020MedFullInntektOgFeriepengerInntektV2Reponse.json")
            val inntektV2Response = objectMapper.readValue<InntektV2Response>(inntektV2ResponseJson)

            every { aMeldingInntektClient.hentInntektV2(any(), any(), any()) } returns inntektV2Response

            val forventetÅrsinntekt = 110000

            val årsinntekt =
                inntektService.hentÅrsinntektV2(
                    personIdent = personIdent,
                    årstallIFjor = 2020,
                )

            assertEquals(forventetÅrsinntekt, årsinntekt)
        }
    }

    fun lesRessurs(name: String): String {
        val resource =
            this::class.java.classLoader.getResource(name)
                ?: throw IllegalArgumentException("Resource not found: $name")
        return resource.readText(StandardCharsets.UTF_8)
    }
}
