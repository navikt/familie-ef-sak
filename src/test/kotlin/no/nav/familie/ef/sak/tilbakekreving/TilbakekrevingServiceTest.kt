package no.nav.familie.ef.sak.tilbakekreving

import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingServiceTest {


    private val tilbakekrevingRepository = mockk<TilbakekrevingRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val tilbakekrevingClient = mockk<TilbakekrevingClient>()
    private val simuleringService = mockk<SimuleringService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val tilbakekrevingService =
            TilbakekrevingService(tilbakekrevingRepository,
                                  behandlingService,
                                  fagsakService,
                                  tilbakekrevingClient,
                                  simuleringService,
                                  arbeidsfordelingService)

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler(true) } returns "bob"
    }

    @AfterAll
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class LagreTilbakekreving {

        @Test
        internal fun `skal ikke være mulig å lagre tilbakekreving for låst behandlig `() {
            every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak(), status = FERDIGSTILT)
            val tilbakekrevingDto =
                    TilbakekrevingDto(valg = Tilbakekrevingsvalg.AVVENT,
                                      varseltekst = "",
                                      begrunnelse = "Dette er tekst ")
            val feil = assertThrows<Feil> {
                tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto,
                                                          behandlingId = UUID.randomUUID())
            }
            assertThat(feil.message).isEqualTo("Behandlingen er låst for redigering")
        }

        @Test
        internal fun `Skal kaste feil dersom vi forsøker lagre tilbakekreving med varsl som mangler varseltekst`() {
            every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak())
            val tilbakekrevingDto =
                    TilbakekrevingDto(valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                                      varseltekst = null,
                                      begrunnelse = "tekst her")

            val feil = assertThrows<Feil> {
                tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto,
                                                          behandlingId = UUID.randomUUID())
            }
            assertThat(feil.message).isEqualTo("Må fylle ut varseltekst for å lage tilbakekreving med varsel")
        }


        @Test
        internal fun `Skal kaste feil dersom vi forsøker lagre tilbakekreving med varsel som har tom varseltekst`() {
            every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak())
            val tilbakekrevingDto =
                    TilbakekrevingDto(valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                                      varseltekst = "   ",
                                      begrunnelse = "tekst her")

            val feil = assertThrows<Feil> {
                tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto,
                                                          behandlingId = UUID.randomUUID())
            }
            assertThat(feil.message).isEqualTo("Må fylle ut varseltekst for å lage tilbakekreving med varsel")
        }

        @Test
        internal fun `Skal lagre forventet tilbakekreving entitet når alt går bra`() {
            every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak())
            val tilbakekrevingSlot = slot<Tilbakekreving>()
            val forventetBegrunnelse = "tekst her"
            val varseltekst = "Dette er en varseltekst"
            val tilbakekrevingDto =
                    TilbakekrevingDto(valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                                      varseltekst = varseltekst,
                                      begrunnelse = forventetBegrunnelse)

            val behandlingId = UUID.randomUUID()
            every { tilbakekrevingRepository.deleteById(any()) } just Runs
            every { tilbakekrevingRepository.insert(capture(tilbakekrevingSlot)) } answers { firstArg() }
            tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto, behandlingId = behandlingId)
            assertThat(tilbakekrevingSlot.captured.behandlingId).isEqualTo(behandlingId)
            assertThat(tilbakekrevingSlot.captured.begrunnelse).isEqualTo(forventetBegrunnelse)
        }
    }

    @Nested
    inner class GenererBrev {

        val simuleringsoppsummering = Simuleringsoppsummering(
                perioder = listOf(),
                fomDatoNestePeriode = null,
                etterbetaling = BigDecimal.valueOf(5000),
                feilutbetaling = BigDecimal.valueOf(40_000),
                fom = LocalDate.of(2021, 1, 1),
                tomDatoNestePeriode = null,
                forfallsdatoNestePeriode = null,
                tidSimuleringHentet = LocalDate.of(2021, 11, 1),
                tomSisteUtbetaling = LocalDate.of(2021, 10, 31)
        )

        @Test
        internal fun `Varselbrev må lages med riktig varseltekst`() {
            val requestSlot = mockHentDataForGenereringAvVarselbrev()
            tilbakekrevingService.genererBrev(UUID.randomUUID(), "Varsel, varsel")
            assertThat(requestSlot.captured.varseltekst).isEqualTo("Varsel, varsel")
        }

        @Test
        internal fun `Varselbrev feiler hvis behandling er låst`() {
            every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak(), status = FERDIGSTILT)
            val assertFails = assertFailsWith<Feil> { tilbakekrevingService.genererBrev(UUID.randomUUID(), "Varsel, varsel") }
            assertThat(assertFails.frontendFeilmelding).isEqualTo("Kan ikke generere forhåndsvisning av varselbrev på en ferdigstilt behandling.")
        }

        private fun mockHentDataForGenereringAvVarselbrev(): CapturingSlot<ForhåndsvisVarselbrevRequest> {

            val fagsak = fagsak(identer = setOf(PersonIdent("12345678901")))
            every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak
            every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak)

            every { simuleringService.simuler(any()) } returns simuleringsoppsummering
            every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet("123", "123")

            val request = slot<ForhåndsvisVarselbrevRequest>()
            every { tilbakekrevingClient.hentForhåndsvisningVarselbrev(capture(request)) } returns "bytearray".toByteArray()
            return request
        }
    }

    @Nested
    inner class OpprettManuellTilbakekreving {

        @Test
        fun `feiler hvis tilbakekrevingsbehandling ikke kan opprettes`() {
            val fagsak = fagsak(identer = setOf(PersonIdent("12345678901")))
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            every { tilbakekrevingClient.kanBehandlingOpprettesManuelt(fagsak.stønadstype, fagsak.eksternId.id) }
                    .returns(KanBehandlingOpprettesManueltRespons(false, "Melding til front end."))

            val feil = assertFailsWith<Feil> {
                tilbakekrevingService.opprettManuellTilbakekreving(fagsak.id)
            }

            assertThat(feil.frontendFeilmelding).isEqualTo("Melding til front end.")
        }

        @Test
        fun `fullfører kaller tilbakekrevingClient opprettManuelTilbakekreving hvis behandling kan opprettes`() {
            val fagsak = fagsak(identer = setOf(PersonIdent("12345678901")))
            every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
            val behandling = behandling(fagsak)
            every { behandlingService.hentSisteBehandling(fagsak.id) } returns behandling
            every { tilbakekrevingClient.kanBehandlingOpprettesManuelt(fagsak.stønadstype, fagsak.eksternId.id) }
                    .returns(KanBehandlingOpprettesManueltRespons(true, "Ok."))
            every {
                tilbakekrevingClient.opprettManuelTilbakekreving(fagsak.eksternId.id,
                                                                 behandling.eksternId.id,
                                                                 fagsak.stønadstype)
            } just runs

            tilbakekrevingService.opprettManuellTilbakekreving(fagsak.id)

            verify { tilbakekrevingClient.opprettManuelTilbakekreving(fagsak.eksternId.id,
                                                                      behandling.eksternId.id,
                                                                      fagsak.stønadstype) }
        }


    }

}

