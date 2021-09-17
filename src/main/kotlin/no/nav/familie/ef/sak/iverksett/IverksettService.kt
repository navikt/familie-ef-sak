package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.fagsak.Fagsak
import no.nav.familie.ef.sak.task.StartBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class IverksettService(private val taskRepository: TaskRepository) {

    fun startBehandling(fagsak: Fagsak) {
        taskRepository.save(StartBehandlingTask.opprettTask(fagsakId = fagsak.id, personIdent = fagsak.hentAktivIdent() ))
    }
}