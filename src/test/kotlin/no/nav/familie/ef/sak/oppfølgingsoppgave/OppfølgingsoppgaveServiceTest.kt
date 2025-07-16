package no.nav.familie.ef.sak.no.nav.familie.ef.sak.oppfølgingsoppgave

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseRepository
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingRepository
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevmottakereService
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.ekstern.stønadsperiode.EksternStønadsperioderService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattRevurdering
import no.nav.familie.ef.sak.infotrygd.LøpendeOvergangsstønadAktivitetsperioder
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev.AutomatiskBrevRepository
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForOpprettelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OppfølgingsoppgaveServiceTest {
    private val oppgaverForOpprettelseRepository = mockk<OppgaverForOpprettelseRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val behandlingService = mockk<BehandlingService>()
    private val vedtakService = mockk<VedtakService>()
    private val oppgaverForFerdigstillingRepository = mockk<OppgaverForFerdigstillingRepository>()
    private val automatiskBrevRepository = mockk<AutomatiskBrevRepository>()
    private val iverksettClient = mockk<IverksettClient>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()
    private val brevClient = mockk<BrevClient>()
    private val frittståendeBrevService = mockk<FrittståendeBrevService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val eksternStønadsperioderService = mockk<EksternStønadsperioderService>()
    private val brevmottakereService = mockk<BrevmottakereService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val brevsignaturService = mockk<BrevsignaturService>()

    private var oppfølgingsoppgaveService =
        spyk(
            OppfølgingsoppgaveService(
                oppgaverForFerdigstillingRepository,
                oppgaverForOpprettelseRepository,
                automatiskBrevRepository,
                behandlingService,
                tilkjentYtelseService,
                vedtakService,
                iverksettClient,
                familieDokumentClient,
                brevClient,
                frittståendeBrevService,
                personopplysningerService,
                eksternStønadsperioderService,
                brevmottakereService,
                brevsignaturService,
                fagsakService,
                behandlingRepository,
            ),
        )

    private val behandling = behandling(fagsak = fagsak())
    private val behandlingId = behandling.id
    private val oppgaverForOpprettelse = OppgaverForOpprettelse(behandlingId, emptyList())
    private val saksbehandling = lagSaksbehandling(stønadType = StønadType.OVERGANGSSTØNAD, behandling = behandling)
    private val vedtak = mockk<Vedtak>()

    @BeforeEach
    fun init() {
        every { oppgaverForOpprettelseRepository.deleteById(any()) } just runs
        every { oppgaverForOpprettelseRepository.deleteByBehandlingId(any()) } just runs
        every { oppgaverForOpprettelseRepository.insert(any()) } returns oppgaverForOpprettelse
        every { oppgaverForOpprettelseRepository.update(any()) } returns oppgaverForOpprettelse
        every { vedtak.resultatType } returns ResultatType.INNVILGE
        every { vedtakService.hentVedtak(any()) } returns vedtak
    }

    @Test
    fun `slett innslag når det ikke kan opprettes noen oppgaver og det finnes innslag fra før`() {
        every { oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(any()) } returns emptyList()
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns true
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling

        opprettTomListeForOppgavetyperSomSkalOpprettes(behandlingId)

        verify { oppgaverForOpprettelseRepository.deleteById(behandlingId) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.insert(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `ikke gjør noe når det ikke kan opprettes oppgaver og det ikke finnes innslag fra før`() {
        every { oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(any()) } returns emptyList()
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns false

        opprettTomListeForOppgavetyperSomSkalOpprettes(behandlingId)

        verify { oppgaverForOpprettelseRepository.deleteById(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.insert(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `oppdater innslag når det finnes innslag, og når man kan oppdatere oppgaver `() {
        every { oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(any()) } returns
            listOf(
                OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID,
            )
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns true

        opprettTomListeForOppgavetyperSomSkalOpprettes(behandlingId)

        verify { oppgaverForOpprettelseRepository.deleteByBehandlingId(any()) }
        verify { oppgaverForOpprettelseRepository.insert(any()) }
    }

    @Test
    fun `lag innslag når det ikke finnes innslag, og når man kan oppdatere oppgaver`() {
        every { oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(any()) } returns
            listOf(
                OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID,
            )
        every { oppgaverForOpprettelseRepository.existsById(any()) } returns false

        opprettTomListeForOppgavetyperSomSkalOpprettes(behandlingId)

        verify(exactly = 0) { oppgaverForOpprettelseRepository.deleteById(any()) }
        verify { oppgaverForOpprettelseRepository.insert(any()) }
        verify(exactly = 0) { oppgaverForOpprettelseRepository.update(any()) }
    }

    @Test
    fun `skal kunne opprette oppgave hvis behandling er førstegangsbehandling`() {
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue
    }

    @Test
    fun `skal kunne opprette oppgave hvis behandling er en revurdering`() {
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattRevurdering.id) } returns saksbehandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattRevurdering.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue
    }

    @Test
    fun `skal ikke kunne opprette oppgave hvis behandling er førstegangsbehandling, men andeler under 1 år frem i tid`() {
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelseUnder1årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    @Test
    fun `skal ikke kunne opprette fremleggsoppgave hvis stønadstype er skolepenger`() {
        val saksbehandling = lagSaksbehandling(stønadType = StønadType.SKOLEPENGER, behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelseUnder1årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)
        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    @Test
    fun `ikke oppgaveopprettelse for avslått overgangsstønad med tilkjente ytelser under 1 år frem i tid`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelseUnder1årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)

        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse
    }

    @Test
    fun `oppgaveopprettelse for avslått overgangsstønad med tilkjente ytelser over 1 år frem i tid`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)
        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue()
    }

    @Test
    fun `oppgaveopprettelse for avslått overgangsstønad med ytelser frem i tid og avslagsårsak inntektsendringer`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)
        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isTrue()
    }

    @Test
    fun `ikke oppgaveopprettelse for avslått overgangsstønad med avslagsårsak ulik inntektsendring`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.KORTVARIG_AVBRUDD_JOBB
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )

        val oppgaver = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)
        assertThat(oppgaver.contains(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)).isFalse()
    }

    @Test
    fun `siste iverksatte behandling hentes for avslag`() {
        val saksbehandling = lagSaksbehandling(behandling = behandling)
        every { tilkjentYtelseService.hentForBehandlingEllerNull(any()) } returns tilkjentYtelse2årFremITid
        every { behandlingService.hentSaksbehandling(iverksattFørstegangsbehandling.id) } returns saksbehandling
        every { vedtak.avslåÅrsak } returns AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        every { vedtak.resultatType } returns ResultatType.AVSLÅ
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        every { eksternStønadsperioderService.hentOvergangsstønadperioderMedAktivitet(any()) } returns
            LøpendeOvergangsstønadAktivitetsperioder(
                personIdent = emptySet(),
                perioder = emptyList(),
            )
        oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettesForOvergangsstønad(iverksattFørstegangsbehandling.id)
        verify { behandlingService.finnSisteIverksatteBehandling(any()) }
    }

    private val tilkjentYtelse2årFremITid =
        lagTilkjentYtelse(
            andelerTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        fraOgMed = LocalDate.now(),
                        kildeBehandlingId = UUID.randomUUID(),
                        beløp = 10_000,
                        tilOgMed = LocalDate.now().plusYears(2),
                    ),
                ),
        )

    private val tilkjentYtelseUnder1årFremITid =
        lagTilkjentYtelse(
            andelerTilkjentYtelse =
                listOf(
                    lagAndelTilkjentYtelse(
                        fraOgMed = LocalDate.now(),
                        kildeBehandlingId = UUID.randomUUID(),
                        beløp = 10_000,
                        tilOgMed = LocalDate.now().plusMonths(11),
                    ),
                ),
        )

    private fun lagSaksbehandling(
        stønadType: StønadType = StønadType.OVERGANGSSTØNAD,
        behandling: Behandling,
    ): Saksbehandling {
        val fagsak = fagsak(stønadstype = stønadType)
        return saksbehandling(fagsak, behandling)
    }

    private fun opprettTomListeForOppgavetyperSomSkalOpprettes(behandlingId: UUID) =
        oppfølgingsoppgaveService.lagreOppgaverForOpprettelse(
            saksbehandling,
            data =
                SendTilBeslutterDto(
                    emptyList(),
                ),
        )
}
