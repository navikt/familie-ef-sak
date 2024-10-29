package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.infotrygd.Saktreff
import no.nav.familie.kontrakter.ef.infotrygd.Vedtakstreff
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class InfotrygdServiceTest {
    private val personService = mockk<PersonService>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val infotrygdService = InfotrygdService(infotrygdReplikaClient, personService)

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
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.OVERGANGSSTØNAD))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.BARNETILSYN))).isFalse
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.BARNETILSYN, StønadType.SKOLEPENGER))).isFalse
    }

    @Test
    internal fun `person har treff i sak om barnetilsyn`() {
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } answers {
            InfotrygdFinnesResponse(emptyList(), listOf(Saktreff(ident, StønadType.BARNETILSYN)))
        }
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.BARNETILSYN))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.BARNETILSYN, StønadType.OVERGANGSSTØNAD))).isTrue
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.OVERGANGSSTØNAD))).isFalse
        assertThat(infotrygdService.eksisterer(ident, setOf(StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER))).isFalse
    }

    @Test
    internal fun `finner inget vedtak eller sak`() {
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } answers {
            InfotrygdFinnesResponse(emptyList(), emptyList())
        }
        StønadType.values().forEach {
            assertThat(infotrygdService.eksisterer(ident, setOf(it))).isFalse
        }
    }

    @Test
    internal fun `finnSaker - skal sortere de vedtaksdato desc, mottattDato desc`() {
        fun sak(
            vedtaksdato: LocalDate?,
            mottattDato: LocalDate?,
        ) = InfotrygdSak(
            "1",
            resultat = InfotrygdSakResultat.INNVILGET,
            stønadType = StønadType.OVERGANGSSTØNAD,
            vedtaksdato = vedtaksdato,
            mottattDato = mottattDato,
        )

        val a = sak(LocalDate.of(2021, 1, 1), null)
        val b = sak(LocalDate.of(2021, 3, 1), null)
        val c = sak(null, LocalDate.of(2021, 3, 1))
        val d = sak(null, LocalDate.of(2021, 1, 1))
        every { infotrygdReplikaClient.hentSaker(any()) } returns InfotrygdSakResponse(listOf(a, b, c, d))

        assertThat(infotrygdService.hentSaker(ident).saker).containsExactly(c, d, b, a)
    }

    private fun mockPdl(historiskIdent: String? = null) {
        val pdlIdenter = mutableListOf(PdlIdent(ident, false))
        if (historiskIdent != null) {
            pdlIdenter.add(PdlIdent(historiskIdent, true))
        }
        every { personService.hentPersonIdenter(ident) } returns PdlIdenter(pdlIdenter)
    }
}
