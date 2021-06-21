package no.nav.familie.ef.sak.task

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ef.sak.integration.PdlClient
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.iverksett.BehandlingStatistikkDto
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = BehandlingsstatistikkTask.TYPE,
                     beskrivelse = "Sender behandlingsstatistikk til iverksett")

class BehandlingsstatistikkTask(private val iverksettClient: IverksettClient,
                           ) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingsstatistikk = objectMapper.readValue<BehandlingStatistikkDto>(task.payload)

        iverksettClient.sendBehandlingsstatistikk(behandlingsstatistikk);
    }



    companion object {

        fun opprettTask(behandlingsstatistikk: BehandlingStatistikkDto, personIdent: String): Task =
                Task(type = TYPE,
                     payload = objectMapper.writeValueAsString(behandlingsstatistikk))

        const val TYPE = "behandlingsstatistikkTask"
    }

}