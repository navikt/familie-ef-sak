package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.repository.domain.Behandling
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import kotlin.test.assertFailsWith

internal class TilgangServiceTest {


    private val personService: PersonService = mockk()
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val cacheManager = ConcurrentMapCacheManager()
    private val tilgangService =
            TilgangService(integrasjonerClient = familieIntegrasjonerClient,
                           personService = personService,
                           behandlingService = behandlingService,
                           fagsakService = fagsakService,
                           rolleConfig = RolleConfig("", "", ""),
                           cacheManager = cacheManager)
    private val mocketPersonIdent = "12345"

    private val fagsak = fagsak(fagsakpersoner(setOf(mocketPersonIdent)))
    private  val behandling: Behandling = behandling(fagsak)
    private val olaIdent = "4567"
    private val kariIdent = "98765"
    private val barn: Map<String, PdlBarn> = mapOf(Pair(olaIdent, mockk()), Pair(kariIdent, mockk()))

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext("A")
        every { personService.hentIdenterForBarnOgForeldre(any()) } returns listOf(mocketPersonIdent, olaIdent, kariIdent)
        every { behandlingService.hentAktivIdent(behandling.id) } returns fagsak.hentAktivIdent()
        every { fagsakService.hentAktivIdent(fagsak.id) } returns fagsak.hentAktivIdent()
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(false))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        assertFailsWith<ManglerTilgang> { tilgangService.validerTilgangTilPersonMedBarn(mocketPersonIdent) }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        every { personService.hentIdenterForBarnOgForeldre(any()) } returns listOf(mocketPersonIdent, olaIdent, kariIdent)

        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(true))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        tilgangService.validerTilgangTilPersonMedBarn(mocketPersonIdent)
    }

    @Test
    internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til behandling`() {
        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(false))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        assertFailsWith<ManglerTilgang> { tilgangService.validerTilgangTilBehandling(behandling.id) }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(true))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        tilgangService.validerTilgangTilBehandling(behandling.id)
    }

    @Test
    internal fun `validerTilgangTilPersonMedBarn - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(true))

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)
        verify(exactly = 1) {
            personService.hentIdenterForBarnOgForeldre(any())
            familieIntegrasjonerClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilPersonMedBarn - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(true))

        mockBrukerContext("A")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilPersonMedBarn(olaIdent)

        verify(exactly = 2) {
            personService.hentIdenterForBarnOgForeldre(any())
            familieIntegrasjonerClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis to ulike saksbehandler kaller skal den sjekke tilgang på nytt`() {
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(true))

        mockBrukerContext("A")

        tilgangService.validerTilgangTilBehandling(behandling.id)
        tilgangService.validerTilgangTilBehandling(behandling.id)

        verify(exactly = 1) {
            behandlingService.hentAktivIdent(behandling.id)
            personService.hentIdenterForBarnOgForeldre(any())
            familieIntegrasjonerClient.sjekkTilgangTilPersoner(any())
        }
    }

    @Test
    internal fun `validerTilgangTilBehandling - hvis samme saksbehandler kaller skal den ha cachet`() {
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(true))

        mockBrukerContext("A")
        tilgangService.validerTilgangTilBehandling(behandling.id)
        mockBrukerContext("B")
        tilgangService.validerTilgangTilBehandling(behandling.id)

        verify(exactly = 2) {
            behandlingService.hentAktivIdent(behandling.id)
            personService.hentIdenterForBarnOgForeldre(any())
            familieIntegrasjonerClient.sjekkTilgangTilPersoner(any())
        }
    }

}
