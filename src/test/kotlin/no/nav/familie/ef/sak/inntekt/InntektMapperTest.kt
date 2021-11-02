package no.nav.familie.ef.sak.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.felles.kodeverk.CachedKodeverkService
import no.nav.familie.ef.sak.inntekt.ekstern.HentInntektListeResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg.EregService
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class InntektMapperTest {

    private val kodeverkService = mockk<CachedKodeverkService>()
    private val eregService = mockk<EregService>()
    private val inntektMapper = InntektMapper(kodeverkService, eregService)

    @BeforeEach
    internal fun setUp() {
        every { eregService.hentOrganisasjoner(any()) } returns listOf(Organisasjon("805824352", "orgnavn"))
        every { kodeverkService.hentInntekt() } returns
                mapOf(InntektKodeverkType.LOENNSINNTEKT to mapOf("fastloenn" to "Fastlønn"),
                InntektKodeverkType.TILLEGSINFORMASJON_KATEGORI to mapOf("NorskKontinentalsokkel" to "Norsk kontinentalsokkel"))
    }

    @Test
    internal fun `skal mappe om response fra inntekt til inntekt per virksomhet og måned`() {
        val inntektDto = inntektMapper.mapInntekt(lagResponse())
        val inntektForVirksomhet = inntektDto.inntektPerVirksomhet.single { it.identifikator == "805824352" }

        assertThat(inntektDto.inntektPerVirksomhet).hasSize(1)

        val inntektForSeptember = inntektForVirksomhet.inntektPerMåned[YearMonth.of(2021, 9)]!!
        assertThat(inntektForSeptember.totalbeløp).isEqualTo(150)
        assertThat(inntektForSeptember.inntekt).hasSize(2)

        val inntektForOktober = inntektForVirksomhet.inntektPerMåned[YearMonth.of(2021, 10)]!!
        assertThat(inntektForOktober.totalbeløp).isEqualTo(25)
        assertThat(inntektForOktober.inntekt).hasSize(1)
    }

    @Test
    internal fun `Sjekker mapping på alle felt`() {
        val inntektDto = inntektMapper.mapInntekt(lagResponse())
        val inntektForVirksomhet = inntektDto.inntektPerVirksomhet.single()
        val inntekt = inntektForVirksomhet.inntektPerMåned.values.first().inntekt.first()

        assertThat(inntekt.beløp).isEqualTo(50)
        assertThat(inntekt.beskrivelse).isEqualTo("Fastlønn")
        assertThat(inntekt.fordel).isEqualTo(Fordel.KONTANTYTELSE)
        assertThat(inntekt.type).isEqualTo(InntektType.LØNNSINNTEKT)
        assertThat(inntekt.kategori).isEqualTo("Norsk kontinentalsokkel")
        assertThat(inntekt.opptjeningsland).isEqualTo("NO")

        assertThat(inntekt.opptjeningsperiodeFom).isEqualTo(LocalDate.of(2021, 9, 1))
        assertThat(inntekt.opptjeningsperiodeTom).isEqualTo(LocalDate.of(2021, 9, 30))


        assertThat(inntektForVirksomhet.navn).isEqualTo("orgnavn")
        assertThat(inntektForVirksomhet.identifikator).isEqualTo("805824352")

        assertThat(inntektDto.avvik).hasSize(1)
        assertThat(inntektDto.avvik.first()).isEqualTo("805824352 (2021-10) - Feil på innsendt a-melding.")
    }

    private fun lagResponse(): HentInntektListeResponse {
        return objectMapper.readValue(this::class.java.classLoader.getResource("inntekt/inntektResponse.json")!!)
    }


}