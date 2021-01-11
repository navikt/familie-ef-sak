package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.config.KlientValidatorConfig
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.domain.Behandling
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

internal class TilgangServiceTest {


    private val personService: PersonService = mockk()
    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val klientValidatorConfig = KlientValidatorConfig(mapOf(KlientValidatorConfig.Klient.ARENA to "sts/123"))
    private val tilgangService =
            TilgangService(familieIntegrasjonerClient,
                           personService,
                           behandlingService,
                           fagsakService,
                           RolleConfig("", "", ""),
                           klientValidatorConfig)
    private val mocketPersonIdent = "12345"

    val fagsak = fagsak(fagsakpersoner(setOf(mocketPersonIdent)))
    val behandling: Behandling = behandling(fagsak)
    private val olaIdent = "4567"
    private val kariIdent = "98765"
    val barn: Map<String, PdlBarn> = mapOf(Pair(olaIdent, mockk()), Pair(kariIdent, mockk()))


    @Test
    internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        every { personService.hentIdenterForBarnOgForeldre(any()) } returns listOf(mocketPersonIdent, olaIdent, kariIdent)

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
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { personService.hentIdenterForBarnOgForeldre(any()) } returns listOf(mocketPersonIdent, olaIdent, kariIdent)

        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(false))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        assertFailsWith<ManglerTilgang> { tilgangService.validerTilgangTilBehandling(behandling.id) }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { personService.hentIdenterForBarnOgForeldre(any()) } returns listOf(mocketPersonIdent, olaIdent, kariIdent)

        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(true))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        tilgangService.validerTilgangTilBehandling(behandling.id)
    }

}




