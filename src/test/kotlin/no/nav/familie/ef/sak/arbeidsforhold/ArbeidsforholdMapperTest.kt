package no.nav.familie.ef.sak.no.nav.familie.ef.sak.arbeidsforhold

import no.nav.familie.ef.sak.arbeidsforhold.ekstern.Ansettelsesperiode
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.Arbeidsavtaler
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.Arbeidsforhold
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.Arbeidsgiver
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsgiverType
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.Arbeidstaker
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.Periode
import no.nav.familie.ef.sak.arbeidsforhold.tilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsforholdMapperTest {

    @Test
    internal fun `Sjekk mapping av alle felt fra arbeidsforhold til arbeidsforholdDTO`() {
        val arbeidsforholdDtoList = arbeidsforholdList.tilDto()
        assertThat(arbeidsforholdDtoList.size).isEqualTo(2)

        val arbeidsforholdDto = arbeidsforholdDtoList.first()
        assertThat(arbeidsforholdDto.navArbeidsforholdId).isEqualTo(1L)
        assertThat(arbeidsforholdDto.arbeidsforholdId).isEqualTo("1")
        assertThat(arbeidsforholdDto.arbeidstaker?.type).isEqualTo("type")
        assertThat(arbeidsforholdDto.arbeidstaker?.offentligIdent).isEqualTo("offentligIdent")
        assertThat(arbeidsforholdDto.arbeidstaker?.aktoerId).isEqualTo("id")
        assertThat(arbeidsforholdDto.arbeidsgiver?.type).isEqualTo(ArbeidsgiverType.Organisasjon)
        assertThat(arbeidsforholdDto.arbeidsgiver?.organisasjonsnummer).isEqualTo("orgnummer")
        assertThat(arbeidsforholdDto.arbeidsgiver?.offentligIdent).isEqualTo("offentligIdent")
        assertThat(arbeidsforholdDto.type).isEqualTo("type")
        assertThat(arbeidsforholdDto.ansettelsesperiode?.bruksperiode).isEqualTo(bruksperiode)
        assertThat(arbeidsforholdDto.ansettelsesperiode?.periode).isEqualTo(gyldighetsperiode)
        assertThat(arbeidsforholdDto.arbeidsavtaler?.size).isEqualTo(1)
        assertThat(arbeidsforholdDto.arbeidsavtaler?.first()?.arbeidstidsordning).isEqualTo("arbeidstidsordning")
        assertThat(arbeidsforholdDto.arbeidsavtaler?.first()?.yrke).isEqualTo("yrke")
        assertThat(arbeidsforholdDto.arbeidsavtaler?.first()?.stillingsprosent).isEqualTo(20.0)
        assertThat(arbeidsforholdDto.arbeidsavtaler?.first()?.antallTimerPrUke).isEqualTo(37.5)
        assertThat(arbeidsforholdDto.arbeidsavtaler?.first()?.beregnetAntallTimerPrUke).isEqualTo(7.5)
        assertThat(arbeidsforholdDto.arbeidsavtaler?.first()?.bruksperiode).isEqualTo(bruksperiode)
        assertThat(arbeidsforholdDto.arbeidsavtaler?.first()?.gyldighetsperiode).isEqualTo(gyldighetsperiode)

    }

    private val gyldighetsperiode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 11, 18))
    private val bruksperiode = Periode(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 11, 17))

    private val arbeidsforhold1 =
        Arbeidsforhold(
            navArbeidsforholdId = 1L,
            arbeidsforholdId = "1",
            arbeidstaker = Arbeidstaker("type", "offentligIdent", "id"),
            arbeidsgiver = Arbeidsgiver(ArbeidsgiverType.Organisasjon, "orgnummer", "offentligIdent"),
            type = "type",
            ansettelsesperiode = Ansettelsesperiode(gyldighetsperiode, bruksperiode),
            arbeidsavtaler = listOf(
                Arbeidsavtaler(
                    arbeidstidsordning = "arbeidstidsordning",
                    yrke = "yrke",
                    stillingsprosent = 20.0,
                    antallTimerPrUke = 37.5,
                    beregnetAntallTimerPrUke = 7.5,
                    bruksperiode = bruksperiode,
                    gyldighetsperiode = gyldighetsperiode
                )
            )
        )

    private val arbeidsforhold2 =
        Arbeidsforhold(
            navArbeidsforholdId = 2L,
            arbeidsforholdId = "2",
            arbeidstaker = Arbeidstaker("type", "offentligIdent", "id"),
            arbeidsgiver = Arbeidsgiver(ArbeidsgiverType.Organisasjon, "orgnummer", "offentligIdent"),
            type = "type",
            ansettelsesperiode = Ansettelsesperiode(),
            arbeidsavtaler = listOf(Arbeidsavtaler())
        )
    private val arbeidsforholdList = listOf(arbeidsforhold1, arbeidsforhold2)
}