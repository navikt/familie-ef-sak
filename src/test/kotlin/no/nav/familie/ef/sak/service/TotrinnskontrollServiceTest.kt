package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.felles.domain.JsonWrapper
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.TotrinnkontrollStatus
import no.nav.familie.ef.sak.vedtak.dto.ÅrsakUnderkjent
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class TotrinnskontrollServiceTest {
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val tilgangService = mockk<TilgangService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val validerOmregningService = mockk<ValiderOmregningService>(relaxed = true)
    private val totrinnskontrollService =
        TotrinnskontrollService(behandlingshistorikkService, behandlingService, tilgangService, validerOmregningService)

    @BeforeEach
    internal fun setUp() {
        every { tilgangService.harTilgangTilRolle(any()) } returns true
        every { tilkjentYtelseRepository.findByBehandlingId(any()) } returns null
    }

    @Test
    internal fun `skal returnere saksbehandler som sendte behandling til besluttning`() {
        val opprettetAv = "Behandler"
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any()) } returns
            behandlingshistorikk(StegType.SEND_TIL_BESLUTTER, opprettetAv = opprettetAv)
        val response =
            totrinnskontrollService
                .lagreTotrinnskontrollOgReturnerBehandler(
                    saksbehandling(status = BehandlingStatus.UTREDES),
                    BeslutteVedtakDto(false, ""),
                    VedtakErUtenBeslutter(false),
                )
        assertThat(response).isEqualTo(opprettetAv)
    }

    @Test
    internal fun `skal utlede saksbehandler som sendte behandling til besluttning`() {
        val opprettetAv = "Behandler"
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), StegType.SEND_TIL_BESLUTTER) } returns
            behandlingshistorikk(StegType.SEND_TIL_BESLUTTER, opprettetAv = opprettetAv)
        val response =
            totrinnskontrollService
                .hentSaksbehandlerSomSendteTilBeslutter(UUID.randomUUID())
        assertThat(response).isEqualTo(opprettetAv)
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen FERDIGSTILT`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FERDIGSTILT)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen IVERKSETTER_VEDTAK`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.IVERKSETTER_VEDTAK)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen UTREDES og ikke har noen totrinnshistorikk`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns null

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere TOTRINNSKONTROLL_UNDERKJENT når behandlingen UTREDES og vedtak er underkjent`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
            behandlingshistorikk(
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                opprettetAv = "Noe",
                beslutt = BeslutteVedtakDto(false, "begrunnelse"),
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.totrinnskontroll?.begrunnelse).isEqualTo("begrunnelse")
    }

    @Test
    internal fun `skal returnere KAN_FATTE_VEDTAK når behandlingen FATTER_VEDTAK og saksbehandler er utreder og ikke er den som sendte behandlingen til fatte vedtak`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any()) } returns
            behandlingshistorikk(
                steg = StegType.SEND_TIL_BESLUTTER,
                opprettetAv = "Annen saksbehandler",
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
        assertThat(totrinnskontroll.totrinnskontroll).isNull()
    }

    @Test
    internal fun `skal returnere IKKE_AUTORISERT når behandlingen FATTER_VEDTAK og saksbehandler er utreder, men er den som sendte behandlingen til fatte vedtak`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any()) } returns
            behandlingshistorikk(
                steg = StegType.SEND_TIL_BESLUTTER,
                opprettetAv = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.IKKE_AUTORISERT)
        assertThat(totrinnskontroll.totrinnskontroll).isNotNull
    }

    @Test
    internal fun `skal returnere IKKE_AUTORISERT når behandlingen FATTER_VEDTAK og saksbehandler ikke er utreder`() {
        every { tilgangService.harTilgangTilRolle(any()) } returns false
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any()) } returns
            behandlingshistorikk(
                steg = StegType.SEND_TIL_BESLUTTER,
                opprettetAv = "Annen saksbehandler",
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.IKKE_AUTORISERT)
        assertThat(totrinnskontroll.totrinnskontroll).isNotNull
        verify(exactly = 1) { tilgangService.harTilgangTilRolle(any()) }
    }

    @Test
    internal fun `skal kaste feil når BESLUTTE_VEDTAK mangler utfall`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
            behandlingshistorikk(
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                opprettetAv = "Annen saksbehandler",
            )

        assertThat(catchThrowable { totrinnskontrollService.hentTotrinnskontrollStatus(ID) })
            .hasMessageContaining("Har underkjent vedtak - savner metadata")
    }

    @Test
    internal fun `skal kaste feil når behandlingstatus er UTREDES og utfall er GODKJENT`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
            behandlingshistorikk(
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_GODKJENT,
                opprettetAv = "Annen saksbehandler",
            )

        assertThat(catchThrowable { totrinnskontrollService.hentTotrinnskontrollStatus(ID) })
            .hasMessageContaining("Skal ikke kunne være annen status enn UNDERKJENT")
    }

    @Test
    internal fun `skal returnere begrunnelse og årsaker underkjent når vedtak er underkjent`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
            behandlingshistorikk(
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                opprettetAv = "Noe",
                beslutt =
                    BeslutteVedtakDto(
                        godkjent = false,
                        begrunnelse = "begrunnelse",
                        årsakerUnderkjent = listOf(ÅrsakUnderkjent.VEDTAKSBREV, ÅrsakUnderkjent.AKTIVITET),
                    ),
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.totrinnskontroll?.begrunnelse).isEqualTo("begrunnelse")
        assertThat(totrinnskontroll.totrinnskontroll?.årsakerUnderkjent).containsExactlyInAnyOrder(
            ÅrsakUnderkjent.VEDTAKSBREV,
            ÅrsakUnderkjent.AKTIVITET,
        )
    }

    @Test
    internal fun `skal returnere underkjent og årsak simulering når vedtak er underkjent med årsak simulering`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
            behandlingshistorikk(
                steg = StegType.BESLUTTE_VEDTAK,
                utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                opprettetAv = "Noe",
                beslutt =
                    BeslutteVedtakDto(
                        godkjent = false,
                        begrunnelse = "En god begrunnelse",
                        årsakerUnderkjent = listOf(ÅrsakUnderkjent.SIMULERING),
                    ),
            )

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.totrinnskontroll?.begrunnelse).isEqualTo("En god begrunnelse")
        assertThat(totrinnskontroll.totrinnskontroll?.årsakerUnderkjent).containsExactlyInAnyOrder(ÅrsakUnderkjent.SIMULERING)
    }

    private fun behandlingshistorikk(
        steg: StegType,
        utfall: StegUtfall? = null,
        opprettetAv: String,
        beslutt: BeslutteVedtakDto? = null,
    ) = Behandlingshistorikk(
        behandlingId = UUID.randomUUID(),
        steg = steg,
        utfall = utfall,
        opprettetAv = opprettetAv,
        metadata =
            beslutt?.let {
                JsonWrapper(objectMapper.writeValueAsString(it))
            },
    )

    private fun behandling(status: BehandlingStatus) = behandling(fagsak, status)

    companion object {
        private val ID = UUID.randomUUID()
        private val fagsak = fagsak()
    }
}
