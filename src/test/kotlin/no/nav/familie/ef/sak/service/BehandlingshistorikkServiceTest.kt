package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.tilHendelseshistorikkDto
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

    @Autowired private lateinit var behandlingshistorikkService: BehandlingshistorikkService
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @Test
    fun `lagre behandling og hent historikk`() {

        /** Lagre */
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val behandlingHistorikk =
                behandlingshistorikkRepository.insert(Behandlingshistorikk(behandlingId = behandling.id,
                                                                           steg = behandling.steg,
                                                                           opprettetAvNavn = "Saksbehandlernavn",
                                                                           opprettetAv = SikkerhetContext.hentSaksbehandler()))
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
                behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id, StegType.BESLUTTE_VEDTAK)
        assertThat(siste).isNull()

        siste = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id, behandling.steg)
        assertThat(siste!!.opprettetAvNavn).isEqualTo("B")
    }

    private fun insert(behandling: Behandling,
                       opprettetAv: String,
                       endretTid: LocalDateTime) {
        behandlingshistorikkRepository.insert(Behandlingshistorikk(behandlingId = behandling.id,
                                                                   steg = behandling.steg,
                                                                   opprettetAvNavn = opprettetAv,
                                                                   endretTid = endretTid))
    }


}



