package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeOvergangsstønad
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PerioderOvergangsstønadServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val perioderOvergangsstønadService = PerioderOvergangsstønadService(infotrygdReplikaClient, pdlClient)

    @Test
    internal fun `hentPerioder henter perioder fra infotrygd med alle identer fra pdl`() {
        val ident = "01234567890"
        val historiskIdent = "01234567890"
        val fomDato = LocalDate.MIN
        val tomDato = LocalDate.MAX
        val request = PerioderOvergangsstønadRequest(ident, fomDato, tomDato)

        every { pdlClient.hentPersonidenter(ident, true) } returns
                PdlIdenter(listOf(PdlIdent(ident, false), PdlIdent(historiskIdent, true)))
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

    private fun infotrygdResponse(vararg infotrygdPeriodeOvergangsstønad: InfotrygdPeriodeOvergangsstønad) =
            InfotrygdPerioderOvergangsstønadResponse(infotrygdPeriodeOvergangsstønad.toList())
}