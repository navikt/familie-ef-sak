package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Stønadstype.BARNETILSYN
import no.nav.familie.ef.sak.repository.domain.Stønadstype.OVERGANGSSTØNAD
import no.nav.familie.ef.sak.repository.domain.Stønadstype.SKOLEPENGER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.*

internal class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByFagsakId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingRepository.findByFagsakId(UUID.randomUUID())).isEmpty()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).containsOnly(behandling)
    }

    @Test
    internal fun findByFagsakIdAndAktivIsTrue() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak, aktiv = false))

        assertThat(behandlingRepository.findByFagsakIdAndAktivIsTrue(UUID.randomUUID())).isNull()
        assertThat(behandlingRepository.findByFagsakIdAndAktivIsTrue(fagsak.id)).isNull()

        val aktivBehandling = behandlingRepository.insert(behandling(fagsak, aktiv = true))
        assertThat(behandlingRepository.findByFagsakIdAndAktivIsTrue(fagsak.id)).isEqualTo(aktivBehandling)
    }

    @Test
    internal fun findByFagsakAndStatus() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.OPPRETTET))

        assertThat(behandlingRepository.findByFagsakIdAndStatus(UUID.randomUUID(), BehandlingStatus.OPPRETTET)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, BehandlingStatus.FERDIGSTILT)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, BehandlingStatus.OPPRETTET)).containsOnly(behandling)
    }

    @Test
    internal fun finnMedEksternId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val findByBehandlingId = behandlingRepository.findById(behandling.id)
        val findByEksternId = behandlingRepository.finnMedEksternId(behandling.eksternId.id)
                              ?: throw error("Behandling med id ${behandling.eksternId.id} finnes ikke")

        assertThat(findByEksternId).isEqualTo(behandling)
        assertThat(findByEksternId).isEqualTo(findByBehandlingId.get())
    }

    @Test
    internal fun `finnFnrForBehandlingId(sql) skal finne gjeldende fnr for behandlingsid`() {
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson(ident = "1"),
                                                          FagsakPerson(ident = "2", sporbar = Sporbar(opprettetTid = LocalDateTime.now().plusDays(2))),
                                                          FagsakPerson(ident = "3"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val fnr = behandlingRepository.finnAktivIdent(behandling.id)
        assertThat(fnr).isEqualTo("2")
    }

    @Test
    internal fun `finnMedEksternId skal gi null når det ikke finnes behandling for gitt id`() {
        val findByEksternId = behandlingRepository.finnMedEksternId(1000000L)
        assertThat(findByEksternId).isEqualTo(null)
    }

    @Test
    internal fun `eksistererBehandlingSomIkkeErBlankett`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingRepository.eksistererBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter)).isTrue
        assertThat(behandlingRepository.eksistererBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, setOf("3"))).isFalse
        assertThat(behandlingRepository.eksistererBehandlingSomIkkeErBlankett(BARNETILSYN, personidenter)).isFalse
    }

    @Test
    internal fun `skal ikke returnere behandling hvis det er blankett`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT))

        assertThat(behandlingRepository.eksistererBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter)).isFalse
    }

    @Test
    internal fun `skal finne nyeste behandlingId`() {
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val annullertFørstegangsbehandling = behandling(fagsak).copy(type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.ANNULLERT, sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(4)))
        val førstegangsbehandling = behandling(fagsak).copy(type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET, sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(3)))
        val blankett = behandling(fagsak).copy(type = BehandlingType.BLANKETT, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET, sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(2)))
        val annullertRevurdering = behandling(fagsak).copy(type = BehandlingType.REVURDERING, status = BehandlingStatus.FERDIGSTILT,resultat = BehandlingResultat.ANNULLERT, sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(1)))
        val revurderingUnderArbeid = behandling(fagsak).copy(type = BehandlingType.REVURDERING,status = BehandlingStatus.IVERKSETTER_VEDTAK, resultat = BehandlingResultat.INNVILGET)
        behandlingRepository.insert(annullertFørstegangsbehandling)
        behandlingRepository.insert(førstegangsbehandling)
        behandlingRepository.insert(blankett)
        behandlingRepository.insert(annullertRevurdering)
        behandlingRepository.insert(revurderingUnderArbeid)
        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(revurderingUnderArbeid.id)
        assertThat(sisteIverksatteBehandling).isEqualTo(førstegangsbehandling.id)
    }

    @Test
    internal fun `skal ikke finnes noen siste iverksatte behandlingId når en førstegangsbehandling iverksettes`() {
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val førstegangsbehandling = behandling(fagsak).copy(type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.IVERKSETTER_VEDTAK, resultat = BehandlingResultat.INNVILGET)
        behandlingRepository.insert(førstegangsbehandling)
        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(førstegangsbehandling.id)
        assertThat(sisteIverksatteBehandling).isNull()
    }
}