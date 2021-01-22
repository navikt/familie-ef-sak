package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.api.dto.TotrinnkontrollStatus
import no.nav.familie.ef.sak.api.dto.TotrinnskontrollDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.JsonWrapper
import no.nav.familie.ef.sak.repository.domain.StegUtfall
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.util.*

internal class TotrinnskontrollServiceTest {

    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val behandlingService = mockk<BehandlingService>()
    private val totrinnskontrollService = TotrinnskontrollService(behandlingshistorikkService, behandlingService)

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen FERDIGSTILT`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FERDIGSTILT)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.begrunnelse).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen IVERKSETTER_VEDTAK`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.IVERKSETTER_VEDTAK)

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.begrunnelse).isNull()
    }

    @Test
    internal fun `skal returnere UAKTUELT når behandlingen UTREDES og ikke har noen totrinnshistorikk`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns null

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.UAKTUELT)
        assertThat(totrinnskontroll.begrunnelse).isNull()
    }

    @Test
    internal fun `skal returnere TOTRINNSKONTROLL_UNDERKJENT når behandlingen UTREDES og vedtak er underkjent`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
                behandlingshistorikk(steg = StegType.BESLUTTE_VEDTAK,
                                     utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                                     opprettetAv = "Noe",
                                     totrinnskontroll = TotrinnskontrollDto(false, "begrunnelse"))

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT)
        assertThat(totrinnskontroll.begrunnelse).isEqualTo("begrunnelse")
    }

    @Test
    internal fun `skal returnere KAN_FATTE_VEDTAK når behandlingen FATTER_VEDTAK og saksbehandler er utreder og ikke er den som sendte behandlingen til fatte vedtak`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
                behandlingshistorikk(steg = StegType.BESLUTTE_VEDTAK,
                                     utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                                     opprettetAv = "Annen saksbehandler")

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.KAN_FATTE_VEDTAK)
        assertThat(totrinnskontroll.begrunnelse).isNull()
    }

    @Test
    internal fun `skal returnere IKKE_AUTORISERT når behandlingen FATTER_VEDTAK og saksbehandler er utreder, men er den som sendte behandlingen til fatte vedtak`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.FATTER_VEDTAK)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
                behandlingshistorikk(steg = StegType.BESLUTTE_VEDTAK,
                                     utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                                     opprettetAv = SikkerhetContext.hentSaksbehandler())

        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(ID)
        assertThat(totrinnskontroll.status).isEqualTo(TotrinnkontrollStatus.IKKE_AUTORISERT)
        assertThat(totrinnskontroll.begrunnelse).isNull()
    }

    @Test
    internal fun `skal kaste feil når BESLUTTE_VEDTAK mangler utfall`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
                behandlingshistorikk(steg = StegType.BESLUTTE_VEDTAK,
                                     utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
                                     opprettetAv = "Annen saksbehandler")

        assertThat(catchThrowable { totrinnskontrollService.hentTotrinnskontrollStatus(ID) })
                .hasMessageContaining("Har underkjent vedtak - savner metadata")
    }

    @Test
    internal fun `skal kaste feil når behandlingstatus er UTREDES og utfall er GODKJENT`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(BehandlingStatus.UTREDES)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns
                behandlingshistorikk(steg = StegType.BESLUTTE_VEDTAK,
                                     utfall = StegUtfall.BESLUTTE_VEDTAK_GODKJENT,
                                     opprettetAv = "Annen saksbehandler")

        assertThat(catchThrowable { totrinnskontrollService.hentTotrinnskontrollStatus(ID) })
                .hasMessageContaining("Skal ikke kunne være annen status enn UNDERKJENT")
    }

    private fun behandlingshistorikk(steg: StegType,
                                     utfall: StegUtfall,
                                     opprettetAv: String,
                                     totrinnskontroll: TotrinnskontrollDto? = null) =
            Behandlingshistorikk(behandlingId = UUID.randomUUID(),
                                        steg = steg,
                                        utfall = utfall,
                                        opprettetAv = opprettetAv,
                                        metadata = totrinnskontroll?.let {
                                            JsonWrapper(objectMapper.writeValueAsString(it))
                                        })

    private fun behandling(status: BehandlingStatus) = behandling(fagsak, true, status)

    companion object {

        private val ID = UUID.randomUUID()
        private val fagsak = fagsak()
    }
}