package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingshistorikkRepository
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime


internal class BehandlingshistorikkServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingshistorikkService: BehandlingshistorikkService
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `lagre og hent behandling, forvent likhet`() {

        /** Lagre */
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val behandlingHistorikk = behandlingshistorikkRepository.insert(Behandlingshistorikk(behandlingId = behandling.id,
                                                                                             steg = behandling.steg,
                                                                                             opprettetAvNavn = "Saksbehandlernavn",
                                                                                             opprettetAv = SikkerhetContext.hentSaksbehandler()))

        /** Hent */
        val innslag: Behandlingshistorikk = behandlingshistorikkService.finnBehandlingshistorikk(behandling.id)[0]

        assertThat(innslag).isEqualTo(behandlingHistorikk)
    }

    @Test
    internal fun `finn seneste behandlinghistorikk`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        fun insert(behandling: Behandling, s: String, endretTid: LocalDateTime) {
            behandlingshistorikkRepository.insert(Behandlingshistorikk(behandlingId = behandling.id,
                                                                       steg = behandling.steg,
                                                                       opprettetAvNavn = s,
                                                                       endretTid = endretTid))
        }
        insert(behandling, "A", LocalDateTime.now().minusDays(1))
        insert(behandling, "B", LocalDateTime.now().plusDays(1))
        insert(behandling, "C", LocalDateTime.now())
        val siste = behandlingshistorikkRepository.findTopByBehandlingIdOrderByEndretTidDesc(behandlingId = behandling.id)
        assertThat(siste.opprettetAvNavn).isEqualTo("B")
    }


}



