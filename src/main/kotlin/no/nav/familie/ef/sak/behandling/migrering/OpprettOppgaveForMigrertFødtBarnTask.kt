package no.nav.familie.ef.sak.behandling.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.ForberedOppgaverForBarnService
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OppgaveForBarn
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OppgaverForBarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = OpprettOppgaveForMigrertFødtBarnTask.TYPE,
                     maxAntallFeil = 3,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Oppretter oppgave for fødte barn")
class OpprettOppgaveForMigrertFødtBarnTask(
        private val forberedOppgaverForBarnService: ForberedOppgaverForBarnService,
        private val iverksettClient: IverksettClient
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveForMigrertFødtBarnTaskData>(task.payload)
        val oppgaver = data.barn.map {
            val beskrivelse = seksEller12Måneder(it) // lag en liste, 6 og 12
            OppgaveForBarn(
                    behandlingId = data.behandlingId,
                    eksternFagsakId = data.eksternFagsakId,
                    personIdent = data.personIdent,
                    stønadType = data.stønadType.name, // TODO fiks kontrakter
                    beskrivelse = beskrivelse,
                    aktivFra = null // TODO 6 eller 12 måneder
            )
        }
        iverksettClient.sendOppgaverForBarn(OppgaverForBarnDto(oppgaver))
    }

    companion object {

        const val TYPE = "opprettOppgaveForMigrertFødtBarn"

        fun opprettOppgave(data: OpprettOppgaveForMigrertFødtBarnTaskData): Task {
            return Task(TYPE, objectMapper.writeValueAsString(data))
        }
    }
}

data class OpprettOppgaveForMigrertFødtBarnTaskData(val behandlingId: UUID,
                                                    val eksternFagsakId: Long,
                                                    val stønadType: Stønadstype,
                                                    val personIdent: String,
                                                    val barn: List<BarnMinimumDto>)
