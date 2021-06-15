package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus.UTREDES
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Stønadstype.BARNETILSYN
import no.nav.familie.ef.sak.repository.domain.Stønadstype.OVERGANGSSTØNAD
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    private val ident = "123"

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
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, FERDIGSTILT)).isEmpty()
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
                                                          FagsakPerson(ident = "2",
                                                                       sporbar = Sporbar(opprettetTid = LocalDateTime.now()
                                                                               .plusDays(2))),
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
    internal fun `finnSisteBehandlingSomIkkeErBlankett`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter))
                .isEqualTo(behandling)
        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, setOf("3"))).isNull()
        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(BARNETILSYN, personidenter)).isNull()
    }

    @Test
    internal fun `finnSisteBehandlingSomIkkeErBlankett - skal returnere teknisk opphør`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.TEKNISK_OPPHØR))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter))
                .isEqualTo(behandling)
    }

    @Test
    internal fun `skal ikke returnere behandling hvis det er blankett`() {
        val personidenter = setOf("1", "2")
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT))

        assertThat(behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(OVERGANGSSTØNAD, personidenter)).isNull()
    }

    @Test
    internal fun `finnSisteIverksatteBehandling - skal returnere teknisk opphør hvis siste behandling er teknisk opphør`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        val tekniskOpphørBehandling =
                behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, type = BehandlingType.TEKNISK_OPPHØR))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(OVERGANGSSTØNAD, setOf(ident)))
                .isEqualTo(tekniskOpphørBehandling)
    }

    @Test
    internal fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen ikke er ferdigstilt`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = UTREDES,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(OVERGANGSSTØNAD, setOf(ident))).isNull()
    }

    @Test
    internal fun `finnSisteIverksatteBehandling - skal ikke returnere noe hvis behandlingen er type blankett`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               type = BehandlingType.BLANKETT,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(OVERGANGSSTØNAD, setOf(ident))).isNull()
    }

    @Test
    internal fun `finnSisteIverksatteBehandling skal finne id til siste behandling som er ferdigstilt, ikke annulert eller blankett`() {
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val annullertFørstegangsbehandling = behandling(fagsak).copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                     status = FERDIGSTILT,
                                                                     resultat = BehandlingResultat.ANNULLERT,
                                                                     sporbar = Sporbar(opprettetTid = LocalDateTime.now()
                                                                             .minusDays(4)))
        val førstegangsbehandling = behandling(fagsak).copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                            status = FERDIGSTILT,
                                                            resultat = BehandlingResultat.INNVILGET,
                                                            sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(3)))
        val blankett = behandling(fagsak).copy(type = BehandlingType.BLANKETT,
                                               status = FERDIGSTILT,
                                               resultat = BehandlingResultat.INNVILGET,
                                               sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(2)))
        val annullertRevurdering = behandling(fagsak).copy(type = BehandlingType.REVURDERING,
                                                           status = FERDIGSTILT,
                                                           resultat = BehandlingResultat.ANNULLERT,
                                                           sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(1)))
        val revurderingUnderArbeid = behandling(fagsak).copy(type = BehandlingType.REVURDERING,
                                                             status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                             resultat = BehandlingResultat.INNVILGET)
        behandlingRepository.insert(annullertFørstegangsbehandling)
        behandlingRepository.insert(førstegangsbehandling)
        behandlingRepository.insert(blankett)
        behandlingRepository.insert(annullertRevurdering)
        behandlingRepository.insert(revurderingUnderArbeid)
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)).isEqualTo(førstegangsbehandling.id)
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(OVERGANGSSTØNAD, setOf("1"))?.id)
                .isEqualTo(førstegangsbehandling.id)
    }

    @Test
    internal fun `skal ikke finnes noen siste iverksatte behandlingId når en førstegangsbehandling iverksettes`() {
        val fagsak = fagsakRepository.insert(fagsak(setOf(FagsakPerson("1"))))
        val førstegangsbehandling = behandling(fagsak).copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                            status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                                            resultat = BehandlingResultat.INNVILGET)
        behandlingRepository.insert(førstegangsbehandling)
        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(førstegangsbehandling.fagsakId)
        assertThat(sisteIverksatteBehandling).isNull()
    }

    @Test
    internal fun `finnEksterneIder - skal hente eksterne ider`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val eksterneIder = behandlingRepository.finnEksterneIder(setOf(behandling.id))

        assertThat(fagsak.eksternId.id).isNotEqualTo(0L)
        assertThat(behandling.eksternId.id).isNotEqualTo(0L)

        assertThat(eksterneIder).hasSize(1)
        val first = eksterneIder.first()
        assertThat(first.behandlingId).isEqualTo(behandling.id)
        assertThat(first.eksternBehandlingId).isEqualTo(behandling.eksternId.id)
        assertThat(first.eksternFagsakId).isEqualTo(fagsak.eksternId.id)
    }

    @Test
    internal fun `skal finne behandlingsider til behandlinger som er iverksatte`() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        val behandling2 = behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlingerSomIkkeErTekniskOpphør(OVERGANGSSTØNAD)).containsExactly(
                behandling2.id)
    }

    @Test
    internal fun `finnSisteIverksatteBehandlinger - skal ikke finne behandling hvis siste er teknisk opphør`() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak,
                                               status = FERDIGSTILT,
                                               opprettetTid = LocalDateTime.now().minusDays(2)))
        behandlingRepository.insert(behandling(fagsak, status = FERDIGSTILT, type = BehandlingType.TEKNISK_OPPHØR))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlingerSomIkkeErTekniskOpphør(OVERGANGSSTØNAD)).isEmpty()
    }

    @Test
    internal fun `finnSisteIverksatteBehandlinger - skal filtrere vekk blankett før den henter siste behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = FERDIGSTILT,
                                                                opprettetTid = LocalDateTime.now().minusDays(2)))
        behandlingRepository.insert(behandling(fagsak,
                                               type = BehandlingType.BLANKETT,
                                               status = FERDIGSTILT))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlingerSomIkkeErTekniskOpphør(OVERGANGSSTØNAD)).containsExactly(
                behandling.id)
    }

    @Test
    internal fun `finnSisteIverksatteBehandlinger - skal filtrere vekk annulerte behandlinger før den henter siste behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = FERDIGSTILT,
                                                                opprettetTid = LocalDateTime.now().minusDays(2)))
        behandlingRepository.insert(behandling(fagsak, type = BehandlingType.BLANKETT, status = FERDIGSTILT))
        assertThat(behandlingRepository.finnSisteIverksatteBehandlingerSomIkkeErTekniskOpphør(OVERGANGSSTØNAD)).containsExactly(
                behandling.id)
    }
}