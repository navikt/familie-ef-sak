package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.task.StartBehandlingTask
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class IverksettService(private val taskRepository: TaskRepository) {

    fun startBehandling(behandling: Behandling, fagsak: Fagsak) {
        taskRepository.save(StartBehandlingTask.opprettTask(behandlingId = behandling.id,
                                                            fagsakId = fagsak.id,
                                                            personIdent = fagsak.hentAktivIdent()))
    }
}