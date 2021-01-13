package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.*
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PerioderOvergangsstønadServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val perioderOvergangsstønadService = PerioderOvergangsstønadService(infotrygdReplikaClient, pdlClient)

    private val ident = "01234567890"

    @Test
    internal fun `hentPerioder henter perioder fra infotrygd med alle identer fra pdl`() {
        val historiskIdent = "01234567890"
        val fomDato = LocalDate.MIN
        val tomDato = LocalDate.MAX
        val request = PerioderOvergangsstønadRequest(ident, fomDato, tomDato)

        mockPdl(historiskIdent)
        every { infotrygdReplikaClient.hentPerioderOvergangsstønad(any()) } returns
                infotrygdResponse(InfotrygdPeriodeOvergangsstønad(ident, LocalDate.now(), LocalDate.now(), 10f))

        val hentPerioder = perioderOvergangsstønadService.hentPerioder(request)

        assertThat(hentPerioder.perioder).hasSize(1)

        verify(exactly = 1) { pdlClient.hentPersonidenter(ident, true) }
        verify(exactly = 1) {
            val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(setOf(ident, historiskIdent), fomDato, tomDato)
            infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        }
    }

    @Test
    internal fun `eksisterer mapper enums`() {
        val slot = slot<SøkFlereStønaderRequest>()
        mockPdl()
        every { infotrygdReplikaClient.eksistererPerson(capture(slot)) } answers {
            val stønader = firstArg<SøkFlereStønaderRequest>().stønader.map { it to StønadTreff(false, false) }.toMap()
            EksistererStønadResponse(stønader)
        }
        SøknadType.values().forEach {
            val eksisterer = perioderOvergangsstønadService.eksisterer(ident, setOf(it))
            assertThat(eksisterer.keys).containsExactly(it)
            assertThat(slot.captured.stønader)
                    .withFailMessage("Skal kun kalle klienten med $it men ble kallt med ${slot.captured.stønader}")
                    .containsExactly(it.tilStønadType())
        }
    }

    private fun SøknadType.tilStønadType() = StønadType.valueOf(name)

    private fun mockPdl(historiskIdent: String? = null) {
        val pdlIdenter = mutableListOf(PdlIdent(ident, false))
        if (historiskIdent != null) {
            pdlIdenter.add(PdlIdent(historiskIdent, true))
        }
        every { pdlClient.hentPersonidenter(ident, true) } returns PdlIdenter(pdlIdenter)
    }

    private fun infotrygdResponse(vararg infotrygdPeriodeOvergangsstønad: InfotrygdPeriodeOvergangsstønad) =
            InfotrygdPerioderOvergangsstønadResponse(infotrygdPeriodeOvergangsstønad.toList())
}