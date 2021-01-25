package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.EksistererStønadResponse
import no.nav.familie.kontrakter.ef.infotrygd.StønadTreff
import no.nav.familie.kontrakter.ef.infotrygd.SøkFlereStønaderRequest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InfotrygdServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val infotrygdService = InfotrygdService(infotrygdReplikaClient, pdlClient)

    private val ident = "01234567890"

    @Test
    internal fun `eksisterer mapper enums`() {
        val slot = slot<SøkFlereStønaderRequest>()
        mockPdl()
        every { infotrygdReplikaClient.eksistererPerson(capture(slot)) } answers {
            val stønader = firstArg<SøkFlereStønaderRequest>().stønader.map { it to StønadTreff(false, false) }.toMap()
            EksistererStønadResponse(stønader)
        }
        SøknadType.values().forEach {
            val eksisterer = infotrygdService.eksisterer(ident, setOf(it))
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
}