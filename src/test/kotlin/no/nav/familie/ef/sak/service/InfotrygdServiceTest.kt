package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.Saktreff
import no.nav.familie.kontrakter.ef.infotrygd.Vedtakstreff
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InfotrygdServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val infotrygdService = InfotrygdService(infotrygdReplikaClient, pdlClient)

    private val ident = "01234567890"

    @BeforeEach
    internal fun setUp() {
        mockPdl()
    }

    @Test
    internal fun `person har treff i vedtak om overgangsstønad`() {
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } answers {
            InfotrygdFinnesResponse(listOf(Vedtakstreff(ident, StønadType.OVERGANGSSTØNAD, false)), emptyList())
        }
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.OVERGANGSSTØNAD))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.OVERGANGSSTØNAD, SøknadType.SKOLEPENGER))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.BARNETILSYN))).isFalse
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.BARNETILSYN, SøknadType.SKOLEPENGER))).isFalse
    }

    @Test
    internal fun `person har treff i sak om barnetilsyn`() {
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } answers {
            InfotrygdFinnesResponse(emptyList(), listOf(Saktreff(ident, StønadType.BARNETILSYN)))
        }
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.BARNETILSYN))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.BARNETILSYN, SøknadType.OVERGANGSSTØNAD))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.OVERGANGSSTØNAD))).isFalse
        assertThat(infotrygdService.eksisterer(ident, setOf(SøknadType.OVERGANGSSTØNAD, SøknadType.SKOLEPENGER))).isFalse
    }

    @Test
    internal fun `finner inget vedtak eller sak`() {
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } answers {
            InfotrygdFinnesResponse(emptyList(), emptyList())
        }
        SøknadType.values().forEach {
            assertThat(infotrygdService.eksisterer(ident, setOf(it))).isFalse
        }
    }

    private fun mockPdl(historiskIdent: String? = null) {
        val pdlIdenter = mutableListOf(PdlIdent(ident, false))
        if (historiskIdent != null) {
            pdlIdenter.add(PdlIdent(historiskIdent, true))
        }
        every { pdlClient.hentPersonidenter(ident, true) } returns PdlIdenter(pdlIdenter)
    }
}