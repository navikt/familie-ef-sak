package no.nav.familie.ef.sak.no.nav.familie.ef.sak.sigrun

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.sigrun.SigrunService
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektForSkatteordning
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektResponse
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.Skatteordning
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class SigrunServiceTest {
    private val sigrunClient = mockk<SigrunClient>()
    private val fagsakPersonService = mockk<FagsakPersonService>()

    private val sigrunService = SigrunService(sigrunClient, fagsakPersonService)

    @BeforeEach
    fun setup() {
        every { fagsakPersonService.hentAktivIdent(any()) } returns "123"
        val tilOgMedÅr = if (YearMonth.now().month.value < 6) YearMonth.now().year - 2 else YearMonth.now().year - 1

        every { sigrunClient.hentPensjonsgivendeInntekt("123", any()) } answers {
            PensjonsgivendeInntektResponse(
                "123",
                secondArg<Int>(),
                pensjonsgivendeInntekt =
                    listOf(
                        pensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND),
                        pensjonsgivendeInntektForSkatteordning(Skatteordning.SVALBARD),
                    ),
            )
        }

        every { sigrunClient.hentPensjonsgivendeInntekt("123", tilOgMedÅr) } answers {
            PensjonsgivendeInntektResponse(
                "123",
                2022,
                pensjonsgivendeInntekt =
                    listOf(
                        pensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND),
                        pensjonsgivendeInntektForSkatteordning(Skatteordning.SVALBARD, 325_000, 20_000),
                    ),
            )
        }
    }

    @Test
    fun `hent inntekt tilbake til 2017 med svalbard inntekt`() {
        val fagsakId = UUID.randomUUID()
        val pensjonsgivendeInntektVisning = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakId)
        assertThat(pensjonsgivendeInntektVisning.first().næring).isEqualTo(250_000)
        assertThat(pensjonsgivendeInntektVisning.first().person).isEqualTo(100_000)
        assertThat(pensjonsgivendeInntektVisning.first().svalbard?.næring).isEqualTo(70_000)
        assertThat(pensjonsgivendeInntektVisning.first().svalbard?.person).isEqualTo(325_000)
        assertThat(pensjonsgivendeInntektVisning.last().inntektsår).isEqualTo(2017)
    }
}

fun pensjonsgivendeInntektForSkatteordning(
    skatteordning: Skatteordning = Skatteordning.FASTLAND,
    lønnsinntekt: Int = 100_000,
    næringsinntekt: Int = 200_000,
) = PensjonsgivendeInntektForSkatteordning(
    skatteordning,
    LocalDate.now(),
    lønnsinntekt,
    null,
    næringsinntekt,
    50_000,
)
