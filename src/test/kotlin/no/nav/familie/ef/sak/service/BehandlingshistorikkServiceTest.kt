package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.behandlingshistorikk.domain.tilHendelseshistorikkDto
import no.nav.familie.ef.sak.behandlingshistorikk.dto.Hendelse
import no.nav.familie.ef.sak.behandlingshistorikk.dto.HendelseshistorikkDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class BehandlingshistorikkServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @Test
    fun `lagre behandling og hent historikk`() {
        /** Lagre */
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val behandlingHistorikk =
            behandlingshistorikkRepository.insert(
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = behandling.steg,
                    opprettetAvNavn = "Saksbehandlernavn",
                    opprettetAv = SikkerhetContext.hentSaksbehandler()
                )
            )
        val hendelseshistorikkDto = behandlingHistorikk.tilHendelseshistorikkDto(saksbehandling(fagsak, behandling))

        /** Hent */
        val innslag: HendelseshistorikkDto =
            behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(fagsak, behandling))[0]

        assertThat(innslag).isEqualTo(hendelseshistorikkDto)
    }

    @Test
    fun `Finn hendelseshistorikk på behandling uten historikk`() {
        /** Lagre */
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        /** Hent */
        val list = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(fagsak, behandling))

        assertThat(list.isEmpty()).isTrue
    }

    @Test
    internal fun `skal slå sammen hendelser av typen opprettet`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val beslutteVedtak = behandling.copy(steg = StegType.BESLUTTE_VEDTAK)

        insert(behandling.copy(steg = StegType.VILKÅR), LocalDateTime.now().minusDays(8))
        insert(behandling.copy(steg = StegType.VILKÅR), LocalDateTime.now().minusDays(7))
        insert(behandling.copy(steg = StegType.SEND_TIL_BESLUTTER), LocalDateTime.now().minusDays(6))
        insert(beslutteVedtak, LocalDateTime.now().minusDays(5), StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT)
        insert(behandling.copy(steg = StegType.VILKÅR), LocalDateTime.now().minusDays(4))
        insert(behandling.copy(steg = StegType.SEND_TIL_BESLUTTER), LocalDateTime.now().minusDays(3))
        insert(beslutteVedtak, LocalDateTime.now().minusDays(2), StegUtfall.BESLUTTE_VEDTAK_GODKJENT)

        val historikk = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(behandling = behandling))
        assertThat(historikk.map { it.hendelse }).containsExactly(
            Hendelse.VEDTAK_GODKJENT,
            Hendelse.SENDT_TIL_BESLUTTER,
            Hendelse.VEDTAK_UNDERKJENT,
            Hendelse.SENDT_TIL_BESLUTTER,
            Hendelse.OPPRETTET,
        )
    }

    @Test
    internal fun `flere sett og av vent skal ikke slåes sammen`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.VILKÅR))
        insert(behandling, LocalDateTime.now().minusDays(10))
        insert(behandling, LocalDateTime.now().minusDays(8), StegUtfall.SATT_PÅ_VENT)
        insert(behandling, LocalDateTime.now().minusDays(5), StegUtfall.TATT_AV_VENT)
        insert(behandling, LocalDateTime.now().minusDays(3), StegUtfall.SATT_PÅ_VENT)
        insert(behandling, LocalDateTime.now().minusDays(2), StegUtfall.TATT_AV_VENT)

        val historikk = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(behandling = behandling))
        assertThat(historikk.map { it.hendelse }).containsExactly(
            Hendelse.TATT_AV_VENT,
            Hendelse.SATT_PÅ_VENT,
            Hendelse.TATT_AV_VENT,
            Hendelse.SATT_PÅ_VENT,
            Hendelse.OPPRETTET,
        )
    }

    @Test
    internal fun `finn seneste behandlinghistorikk`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        insert(behandling, "A", LocalDateTime.now().minusDays(1))
        insert(behandling, "B", LocalDateTime.now().plusDays(1))
        insert(behandling, "C", LocalDateTime.now())

        val siste = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id)
        assertThat(siste.opprettetAvNavn).isEqualTo("B")
    }

    @Test
    internal fun `finn seneste behandlinghistorikk med type`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        insert(behandling, "A", LocalDateTime.now().minusDays(1))
        insert(behandling, "B", LocalDateTime.now().plusDays(1))
        insert(behandling, "C", LocalDateTime.now())

        var siste =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(
                behandlingId = behandling.id,
                StegType.BESLUTTE_VEDTAK
            )
        assertThat(siste).isNull()

        siste = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id, behandling.steg)
        assertThat(siste!!.opprettetAvNavn).isEqualTo("B")
    }

    private fun insert(
        behandling: Behandling,
        endretTid: LocalDateTime,
        utfall: StegUtfall? = null
    ) {
        insert(behandling, "opprettetAv", endretTid, utfall)
    }

    private fun insert(
        behandling: Behandling,
        opprettetAv: String,
        endretTid: LocalDateTime,
        utfall: StegUtfall? = null
    ) {
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = behandling.steg,
                utfall = utfall,
                opprettetAvNavn = opprettetAv,
                endretTid = endretTid
            )
        )
    }
}
