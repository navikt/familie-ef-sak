package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = StartBehandlingTask.TYPE,
    beskrivelse = "Sender start behandling til Infotrygd",
)
class StartBehandlingTask(
    private val iverksettClient: IverksettClient,
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val stønadType = fagsak.stønadstype

        if (!finnesEnIverksattBehandlingFor(fagsak)) {
            val identer = personService.hentPersonIdenter(fagsak.hentAktivIdent()).identer()
            iverksettClient.startBehandling(OpprettStartBehandlingHendelseDto(identer, stønadType))
        }
    }

    private fun finnesEnIverksattBehandlingFor(fagsak: Fagsak) = behandlingRepository.finnSisteIverksatteBehandling(fagsak.id) != null

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            fagsakId: UUID,
            personIdent: String,
        ): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
                        this["behandlingId"] = behandlingId.toString()
                        this["fagsakId"] = fagsakId.toString()
                        this["personIdent"] = personIdent
                    },
            )

        const val TYPE = "startBehandlingTask"
    }
}
