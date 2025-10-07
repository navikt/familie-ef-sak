package no.nav.familie.ef.sak.arbeidsforhold

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdClient
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsforhold.Ansettelsesperiode
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArbeidsforholdServiceTest {
    val arbeidsforholdClient: ArbeidsforholdClient = mockk()
    val arbeidsforholdService = ArbeidsforholdService(mockk(), arbeidsforholdClient)

    @BeforeEach
    fun setup() {
        every { arbeidsforholdClient.hentArbeidsforhold(any()) } returns
            listOf(
                Arbeidsforhold(
                    id = "123",
                    ansettelsesperiode =
                        Ansettelsesperiode(
                            LocalDate.now().minusYears(2).toString(),
                            null,
                        ),
                ),
            )
    }

    @Test
    fun `finnes avsluttet arbeidsforhold siste 6 mnd`() {
        every { arbeidsforholdClient.hentArbeidsforhold(any()) } returns
            listOf(
                Arbeidsforhold(
                    id = "123",
                    ansettelsesperiode =
                        Ansettelsesperiode(
                            LocalDate.now().minusYears(2).toString(),
                            LocalDate.now().toString(),
                        ),
                ),
            )
        val finnesAvsluttetArbeidsforhold = arbeidsforholdService.finnesAvsluttetArbeidsforholdSisteAntallMåneder("1")
        assertThat(finnesAvsluttetArbeidsforhold).isTrue
    }

    @Test
    fun `finnes ikke avsluttet arbeidsforhold siste 6 mnd`() {
        every { arbeidsforholdClient.hentArbeidsforhold(any()) } returns
            listOf(
                Arbeidsforhold(
                    id = "123",
                    ansettelsesperiode =
                        Ansettelsesperiode(
                            LocalDate.now().minusYears(2).toString(),
                            null,
                        ),
                ),
            )
        val finnesAvsluttetArbeidsforhold = arbeidsforholdService.finnesAvsluttetArbeidsforholdSisteAntallMåneder("1")
        assertThat(finnesAvsluttetArbeidsforhold).isFalse
    }

    @Test
    fun `finnes nytt arbeidsforhold siste 4 mnd`() {
        every { arbeidsforholdClient.hentArbeidsforhold(any()) } returns
            listOf(
                Arbeidsforhold(
                    id = "123",
                    ansettelsesperiode =
                        Ansettelsesperiode(
                            LocalDate.now().minusMonths(2).toString(),
                            null,
                        ),
                ),
            )
        val finnesNyttArbeidsforhold = arbeidsforholdService.finnesNyttArbeidsforholdSisteAntallMåneder("1")
        assertThat(finnesNyttArbeidsforhold).isTrue
    }

    @Test
    fun `finnes ikke nytt arbeidsforhold siste 4 mnd`() {
        every { arbeidsforholdClient.hentArbeidsforhold(any()) } returns
            listOf(
                Arbeidsforhold(
                    id = "123",
                    ansettelsesperiode =
                        Ansettelsesperiode(
                            LocalDate.now().minusMonths(8).toString(),
                            null,
                        ),
                ),
            )
        val finnesNyttArbeidsforhold = arbeidsforholdService.finnesNyttArbeidsforholdSisteAntallMåneder("1")
        assertThat(finnesNyttArbeidsforhold).isFalse
    }
}
