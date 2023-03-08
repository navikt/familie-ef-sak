package no.nav.familie.ef.sak.no.nav.familie.ef.sak.sigrun

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.sigrun.SigrunService
import no.nav.familie.ef.sak.sigrun.ekstern.BeregnetSkatt
import no.nav.familie.ef.sak.sigrun.ekstern.Grunnlag
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.SummertSkattegrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class SigrunServiceTest {

    private val sigrunClient = mockk<SigrunClient>()
    private val fagsakService = mockk<FagsakService>()

    private val sigrunService = SigrunService(sigrunClient, fagsakService)

    @BeforeEach
    fun setup() {
        every { fagsakService.hentAktivIdent(any()) } returns "123"
        every { sigrunClient.hentBeregnetSkatt(any(), any()) } returns listOf(
            BeregnetSkatt("skatteoppgjoersdato", "2022-05-01"),
            BeregnetSkatt("personinntektNaering", "50000"),
            BeregnetSkatt("personinntektLoenn", "50000")
        )
        every { sigrunClient.hentSummertSkattegrunnlag(any(), any()) } returns SummertSkattegrunnlag(
            listOf(),
            svalbardGrunnlag = listOf(
                Grunnlag("personinntektBarePensjonsdel", 50000),
                Grunnlag("svalbardPersoninntektNaering", 50000)
            ),
            "skatteoppgjoersdato"
        )
    }

    @Test
    fun `hent inntekt siste tre år med svalbard inntekt`() {
        val fagsakId = UUID.randomUUID()
        val pensjonsgivendeInntektVisning = sigrunService.hentInntektSisteTreÅr(fagsakId)
        assertThat(pensjonsgivendeInntektVisning.size).isEqualTo(3)
        assertThat(pensjonsgivendeInntektVisning.first().inntektsaar).isEqualTo(YearMonth.now().year - 1)
        assertThat(pensjonsgivendeInntektVisning.first().verdi).isEqualTo(200_000)
        assertThat(pensjonsgivendeInntektVisning.last().inntektsaar).isEqualTo(YearMonth.now().year - 3)
    }
}
