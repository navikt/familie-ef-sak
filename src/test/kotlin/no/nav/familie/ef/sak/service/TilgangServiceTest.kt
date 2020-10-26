package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.domene.SøkerMedBarn
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.kontrakter.felles.PersonIdent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertFailsWith

internal class TilgangServiceTest {


    val personService: PersonService = mockk()
    val familieIntegrasjonerClient: FamilieIntegrasjonerClient = mockk()
    val behandlingService: BehandlingService = mockk()
    val fagsakService: FagsakService = mockk()
    val tilgangService = TilgangService(familieIntegrasjonerClient, personService, behandlingService, fagsakService)
    val mocketPersonIdent = "12345"
    val barn: Map<String, PdlBarn> = mapOf(Pair("45679", mockk()), Pair("98765", mockk()))
    val behandlingIdent = UUID.randomUUID()
    val behandling: Behandling = mockk()
    val fagsakId = UUID.randomUUID()
    val fagsak: Fagsak = mockk()

    @Test
    internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til person eller dets barn`() {
        every { personService.hentPersonMedRelasjoner(any()) } returns SøkerMedBarn(mocketPersonIdent, mockk(), barn)

        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(false))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        assertFailsWith<ManglerTilgang> { tilgangService.validerTilgangTilPersonMedBarn(mocketPersonIdent) }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til person og dets barn`() {
        every { personService.hentPersonMedRelasjoner(any()) } returns SøkerMedBarn(mocketPersonIdent, mockk(), barn)

        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(true))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        tilgangService.validerTilgangTilPersonMedBarn(mocketPersonIdent)
    }

    @Test
    internal fun `skal kaste ManglerTilgang dersom saksbehandler ikke har tilgang til behandling`() {
        every { behandlingService.hentBehandling(behandlingIdent) } returns behandling
        every { behandling.fagsakId } returns fagsakId
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak
        every { fagsak.hentAktivIdent() } returns mocketPersonIdent
        every { personService.hentPersonMedRelasjoner(mocketPersonIdent) } returns SøkerMedBarn(mocketPersonIdent, mockk(), barn)

        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(false))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        assertFailsWith<ManglerTilgang> { tilgangService.validerTilgangTilBehandling(behandlingIdent) }
    }

    @Test
    internal fun `skal ikke feile når saksbehandler har tilgang til behandling`() {
        every { behandlingService.hentBehandling(behandlingIdent) } returns behandling
        every { behandling.fagsakId } returns fagsakId
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak
        every { fagsak.hentAktivIdent() } returns mocketPersonIdent
        every { personService.hentPersonMedRelasjoner(mocketPersonIdent) } returns SøkerMedBarn(mocketPersonIdent, mockk(), barn)

        val tilganger = listOf(Tilgang(true), Tilgang(true), Tilgang(true))
        every { familieIntegrasjonerClient.sjekkTilgangTilPersoner(any()) } returns tilganger

        tilgangService.validerTilgangTilBehandling(behandlingIdent)
    }
}




