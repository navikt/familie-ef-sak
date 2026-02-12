package no.nav.familie.ef.sak.amelding

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.amelding.ekstern.ArbeidOgInntektClient
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.jsonMapper
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.testutil.JsonFilUtil.Companion.lesFil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class InntektServiceTest {
    private val fagsakService: FagsakService = mockk(relaxed = true)
    private val arbeidOgInntektClient: ArbeidOgInntektClient = mockk(relaxed = true)
    private val aMeldingInntektClient: AMeldingInntektClient = mockk(relaxed = true)
    private val fagsakPersonService: FagsakPersonService = mockk(relaxed = true)

    private val inntektService: InntektService =
        InntektService(
            aMeldingInntektClient = aMeldingInntektClient,
            arbeidOgInntektClient = arbeidOgInntektClient,
            fagsakService = fagsakService,
            fagsakPersonService = fagsakPersonService,
        )

    private val fagsakId = UUID.randomUUID()
    private val personIdent = "01010199999"

    @Nested
    inner class ParseInntektV2Reponse {
        @Test
        internal fun `parser generell inntektv2 response med riktig data struktur`() {
            val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektGenerellResponse.json")
            val inntektResponse = jsonMapper.readValue<InntektResponse>(inntektV2ResponseJson)

            val forventetMåned = YearMonth.of(2020, 3)
            val forventetInntektType: InntektType = InntektType.LØNNSINNTEKT

            assertEquals(forventetMåned, inntektResponse.inntektsmåneder[0].måned)
            assertEquals(forventetInntektType, inntektResponse.inntektsmåneder[0].inntektListe[0].type)
        }

        @Test
        internal fun `parser inntektv2 response med forskjellige inntekt typer`() {
            val inntektV2ResponseJson: String = lesFil("json/inntekt/InntektFlereInntektTyperResponse.json")
            val inntektResponse = jsonMapper.readValue<InntektResponse>(inntektV2ResponseJson)

            val forventeteInntektTyper =
                listOf(
                    InntektType.LØNNSINNTEKT,
                    InntektType.NAERINGSINNTEKT,
                    InntektType.YTELSE_FRA_OFFENTLIGE,
                    InntektType.PENSJON_ELLER_TRYGD,
                )

            val faktiskeInntektTyper =
                inntektResponse.inntektsmåneder
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
                lesFil("json/inntekt/InntektFulltÅrMedFeriepenger.json")
            val inntektResponse = jsonMapper.readValue<InntektResponse>(inntektV2ResponseJson)

            every { aMeldingInntektClient.hentInntekt(any(), any(), any()) } returns inntektResponse

            val årsinntekt =
                inntektService.hentÅrsinntekt(
                    personIdent = personIdent,
                    årstallIFjor = 2020,
                )

            assertEquals(130056, årsinntekt)
        }
    }
}
