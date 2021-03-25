package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.Behandlingsjournalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.assertj.core.util.Throwables.getRootCause
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsjournalpostRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var behandlingsjournalpostRepository: BehandlingsjournalpostRepository

    @Test
    internal fun `skal kunne lagre flere journalposter på samme behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling1 = behandlingRepository.insert(behandling(fagsak))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))
        behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandling1.id, "1", Journalposttype.U))
        behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandling1.id, "2", Journalposttype.U))
        // setter inn en på behandling 2
        behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandling2.id, "3", Journalposttype.U))

        assertThat(behandlingsjournalpostRepository.findAllByBehandlingId(behandling1.id)).hasSize(2)
    }

    @Test
    internal fun `skal ikke være mulig å legge inn 2 journalposter med samme journalpostId`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling1 = behandlingRepository.insert(behandling(fagsak))
        behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandling1.id, "1", Journalposttype.U))
        val throwable = catchThrowable {
            behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandling1.id,
                                                                           "1",
                                                                           Journalposttype.U))
        }
        assertThat(getRootCause(throwable)).hasMessageContaining("duplicate key value violates unique constraint")
    }
}