package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentGruppe
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
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

    @Test
    internal fun `hentSammenslåttePerioderSomInternPerioder skal returnere tomt resultat hvis identer er tom`() {
        val perioder = infotrygdService.hentSammenslåttePerioderSomInternPerioder(emptySet())

        assertThat(perioder.overgangsstønad).isEmpty()
        assertThat(perioder.barnetilsyn).isEmpty()
        assertThat(perioder.skolepenger).isEmpty()
    }

    @Test
    internal fun `hentPerioderFraReplika med identer skal returnere tomt resultat hvis identer er tom`() {
        val perioder = infotrygdService.hentPerioderFraReplika(emptySet())

        assertThat(perioder.overgangsstønad).isEmpty()
        assertThat(perioder.barnetilsyn).isEmpty()
        assertThat(perioder.skolepenger).isEmpty()
    }

    @Test
    internal fun `skal ikke sende NPID til infotrygd`() {
        val fnr = "12018012345"
        val npid = "01226512345"
        every { personService.hentPersonIdenter(fnr) } returns
            PdlIdenter(listOf(PdlIdent(fnr, false), PdlIdent(npid, true, PdlIdentGruppe.NPID)))
        val søkRequest = slot<InfotrygdSøkRequest>()
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(capture(søkRequest)) } returns InfotrygdFinnesResponse(emptyList(), emptyList())
        val periodeRequest = slot<InfotrygdPeriodeRequest>()
        every { infotrygdReplikaClient.hentSammenslåttePerioder(capture(periodeRequest)) } returns
            InfotrygdPeriodeResponse(emptyList(), emptyList(), emptyList())

        infotrygdService.eksisterer(fnr)
        infotrygdService.hentSammenslåttePerioderSomInternPerioder(setOf(fnr, npid))

        assertThat(søkRequest.captured.personIdenter).containsExactly(fnr)
        assertThat(periodeRequest.captured.personIdenter).containsExactly(fnr)
    }

    @Test
    internal fun `skal ikke kalle infotrygd for person som kun har NPID`() {
        val npid = "01226512345"
        every { personService.hentPersonIdenter(npid) } returns PdlIdenter(listOf(PdlIdent(npid, false, PdlIdentGruppe.NPID)))

        assertThat(infotrygdService.eksisterer(npid)).isFalse()
        assertThat(infotrygdService.hentSaker(npid).saker).isEmpty()
        assertThat(infotrygdService.hentDtoPerioder(npid).overgangsstønad.perioder).isEmpty()
        verify(exactly = 0) { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) }
        verify(exactly = 0) { infotrygdReplikaClient.hentSaker(any()) }
    }

    private fun mockPdl(historiskIdent: String? = null) {
        val pdlIdenter = mutableListOf(PdlIdent(ident, false))
        if (historiskIdent != null) {
            pdlIdenter.add(PdlIdent(historiskIdent, true))
        }
        every { personService.hentPersonIdenter(ident) } returns PdlIdenter(pdlIdenter)
    }
}
