package no.nav.familie.ef.sak.opplysninger.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsaker
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.folkeregisteridentifikator
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TidligereVedtaksperioderServiceTest {

    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val personService = mockk<PersonService>()
    private val historiskPensjonService = mockk<HistoriskPensjonService>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val infotrygdService = InfotrygdService(infotrygdReplikaClient, personService)

    private val service = TidligereVedtaksperioderService(
        fagsakPersonService,
        fagsakService,
        behandlingService,
        tilkjentYtelseService,
        infotrygdService,
        historiskPensjonService,
    )

    private val infotrygdPeriodeRequestSlot = slot<InfotrygdPeriodeRequest>()

    private val personIdent = folkeregisteridentifikator("1")
    private val identAnnenForelder = folkeregisteridentifikator("2")

    private val fagsakPerson = fagsakPerson(fagsakpersoner(identAnnenForelder.ident))
    private val fagsak = fagsak(person = fagsakPerson)
    private val fagsaker = Fagsaker(fagsak, null, null)
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        every {
            infotrygdReplikaClient.hentPerioder(capture(infotrygdPeriodeRequestSlot))
        } answers { InfotrygdReplikaMock.hentPerioderDefaultResponse(firstArg()) }
        every { personService.hentPersonIdenter(personIdent.ident) } returns
            PdlIdenter(listOf(PdlIdent(personIdent.ident, false)))
        every { historiskPensjonService.hentHistoriskPensjon(any(), any()) } returns
            HistoriskPensjonResponse(false, "")
    }

    @Test
    internal fun `skal sjekke om annen forelder har historikk i ef-sak og infotrygd`() {
        mockTidligereVedtakEfSak(harAndeler = true)

        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent))

        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereOvergangsstønad).isTrue
        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereBarnetilsyn).isTrue
        assertThat(tidligereVedtaksperioder.infotrygd.harTidligereSkolepenger).isFalse

        val sak = tidligereVedtaksperioder.sak ?: error("Forventet at sak ikke er null")
        assertThat(sak.harTidligereOvergangsstønad).isTrue
        assertThat(sak.harTidligereBarnetilsyn).isFalse
        assertThat(sak.harTidligereSkolepenger).isFalse

        assertThat(tidligereVedtaksperioder.historiskPensjon).isFalse

        verify(exactly = 1) { infotrygdReplikaClient.hentPerioder(any()) }
        verify(exactly = 1) { tilkjentYtelseService.hentForBehandling(behandling.id) }
        verify(exactly = 1) {
            historiskPensjonService.hentHistoriskPensjon(personIdent.ident, setOf(personIdent.ident))
        }

        assertThat(infotrygdPeriodeRequestSlot.captured.personIdenter).containsExactly(personIdent.ident)
    }

    @Test
    internal fun `hvis en person ikke har noen aktive andeler så har man ikke tidligere vedtaksperioder i ef`() {
        mockTidligereVedtakEfSak(harAndeler = false)

        val tidligereVedtaksperioder = service.hentTidligereVedtaksperioder(listOf(personIdent))

        val sak = tidligereVedtaksperioder.sak ?: error("Forventet at sak ikke er null")
        assertThat(sak.harTidligereOvergangsstønad).isFalse
        assertThat(sak.harTidligereBarnetilsyn).isFalse
        assertThat(sak.harTidligereSkolepenger).isFalse
    }

    private fun mockTidligereVedtakEfSak(harAndeler: Boolean = true) {
        every { fagsakPersonService.finnPerson(any()) } returns fagsakPerson
        every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
        val andelerTilkjentYtelse =
            if (harAndeler) listOf(lagAndelTilkjentYtelse(100, LocalDate.now(), LocalDate.now())) else emptyList()
        every { tilkjentYtelseService.hentForBehandling(behandling.id) } returns lagTilkjentYtelse(andelerTilkjentYtelse)
    }
}