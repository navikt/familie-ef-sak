package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = StartBehandlingTask.TYPE,
                     beskrivelse = "Sender start behandling til Infotrygd")

class StartBehandlingTask(private val iverksettClient: IverksettClient,
                          private val pdlClient: PdlClient,
                          private val fagsakService: FagsakService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val fagsakId = UUID.fromString(task.payload)
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val stønadType = StønadType.valueOf(fagsak.stønadstype.name)

        val identer = pdlClient.hentPersonidenter(fagsak.hentAktivIdent(), historikk = true).identer.map { it.ident }.toSet()
        iverksettClient.startBehandling(OpprettStartBehandlingHendelseDto(identer, stønadType))
    }

    companion object {

        fun opprettTask(fagsakId: UUID, personIdent: String): Task =
                Task(type = TYPE,
                     payload = fagsakId.toString(),
                     properties = Properties().apply {
                         this["saksbehandler"] = SikkerhetContext.hentSaksbehandler()
                         this["fagsakId"] = fagsakId.toString()
                         this["personIdent"] = personIdent
                     })


        const val TYPE = "startBehandlingTask"
    }


}