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
    private val inntektsårUtenInntekt = 2019 downTo 1990

    @BeforeEach
    fun setup() {
        every { fagsakPersonService.hentAktivIdent(any()) } returns "123"

        every { sigrunClient.hentPensjonsgivendeInntekt(any(), 2022) } returns PensjonsgivendeInntektResponse(
            "123",
            2022,
            pensjonsgivendeInntekt = listOf(
                pensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND),
                pensjonsgivendeInntektForSkatteordning(Skatteordning.SVALBARD, 325_000, 20_000),
            ),
        )
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), 2021) } returns PensjonsgivendeInntektResponse(
            "123",
            2021,
            pensjonsgivendeInntekt = listOf(
                pensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND),
                pensjonsgivendeInntektForSkatteordning(Skatteordning.SVALBARD),
            ),
        )
        every { sigrunClient.hentPensjonsgivendeInntekt(any(), 2020) } returns PensjonsgivendeInntektResponse(
            "123",
            2020,
            pensjonsgivendeInntekt = listOf(
                pensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND),
                pensjonsgivendeInntektForSkatteordning(Skatteordning.SVALBARD),
            ),
        )

        inntektsårUtenInntekt.map {
            every { sigrunClient.hentPensjonsgivendeInntekt(any(), it) } returns PensjonsgivendeInntektResponse("123", it, listOf())
        }

        every { sigrunClient.hentPensjonsgivendeInntekt(any(), 1995) } returns PensjonsgivendeInntektResponse(
            "123",
            1995,
            pensjonsgivendeInntekt = listOf(
                pensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND),
                pensjonsgivendeInntektForSkatteordning(Skatteordning.SVALBARD),
            ),
        )
    }

    @Test
    fun `hent inntekt siste fem år med svalbard inntekt`() {
        val fagsakId = UUID.randomUUID()
        val pensjonsgivendeInntektVisning = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakId)
        // assertThat(pensjonsgivendeInntektVisning.size).isEqualTo(inntektsårUtenInntekt.count() - 6)
        assertThat(pensjonsgivendeInntektVisning.first().inntektsår).isEqualTo(YearMonth.now().year - 1)
        assertThat(pensjonsgivendeInntektVisning.first().næring).isEqualTo(250_000)
        assertThat(pensjonsgivendeInntektVisning.first().person).isEqualTo(100_000)
        assertThat(pensjonsgivendeInntektVisning.first().svalbard?.næring).isEqualTo(70_000)
        assertThat(pensjonsgivendeInntektVisning.first().svalbard?.person).isEqualTo(325_000)
        assertThat(pensjonsgivendeInntektVisning.last().inntektsår).isEqualTo(1995)
    }
}

fun pensjonsgivendeInntektForSkatteordning(skatteordning: Skatteordning = Skatteordning.FASTLAND, lønnsinntekt: Int = 100_000, næringsinntekt: Int = 200_000) = PensjonsgivendeInntektForSkatteordning(
    skatteordning,
    LocalDate.now(),
    lønnsinntekt,
    null,
    næringsinntekt,
    50_000,
)
