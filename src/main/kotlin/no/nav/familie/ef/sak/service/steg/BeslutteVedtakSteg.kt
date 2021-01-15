package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.task.IverksettMotOppdragTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class BeslutteVedtakSteg(val taskRepository: TaskRepository, val fagsakService: FagsakService) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {

    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        // Hvis godkjent
        val task = IverksettMotOppdragTask.opprettTask(behandling, fagsak.hentAktivIdent(), SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }

}