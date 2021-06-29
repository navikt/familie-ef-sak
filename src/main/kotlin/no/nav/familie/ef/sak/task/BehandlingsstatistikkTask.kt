package no.nav.familie.ef.sak.task

import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDateTime
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsstatistikkDto
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
        taskStepType = BehandlingsstatistikkTask.TYPE,
        beskrivelse = "Sender behandlingsstatistikk til iverksett"
)

class BehandlingsstatistikkTask(private val iverksettClient: IverksettClient,
                                private val behandlingService: BehandlingService,
                                private val fagsakService: FagsakService,
                                private val søknadService: SøknadService,
                                private val vedtakService: VedtakService,
                                private val oppgaveService: OppgaveService,
                                private val personService: PersonService
) : AsyncTaskStep {
    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId) = objectMapper.readValue<BehandlingsstatistikkTaskPayload>(
                task.payload
        )

        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
        val resultatBegrunnelse = finnResultatBegrunnelse(hendelse, behandlingId)
        val søker = personService.hentSøker(personIdent);
        val søknadstidspunkt = finnSøknadstidspunkt(fagsak, behandlingId)

        val behandlingsstatistikkDto = BehandlingsstatistikkDto(
                behandlingId = behandlingId,
                personIdent = personIdent,
                gjeldendeSaksbehandlerId = gjeldendeSaksbehandler,
                eksternFagsakId = fagsak.eksternId.id.toString(),
                hendelseTidspunkt = hendelseTidspunkt.atZone(zoneIdOslo),
                søknadstidspunkt = søknadstidspunkt.atZone(zoneIdOslo),
                hendelse = hendelse,
                behandlingResultat = behandling.resultat.name,
                resultatBegrunnelse = resultatBegrunnelse,
                opprettetEnhet = sisteOppgaveForBehandling.opprettetAvEnhetsnr ?: "9999",
                ansvarligEnhet = sisteOppgaveForBehandling.tildeltEnhetsnr ?: "9999",
                strengtFortroligAdresse = søker.adressebeskyttelse.gjeldende()?.gradering == AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                stønadstype = StønadType.valueOf(fagsak.stønadstype.name),
                behandlingstype = BehandlingType.valueOf(behandling.type.name)
        )

        iverksettClient.sendBehandlingsstatistikk(behandlingsstatistikkDto);
    }

    private fun finnSisteOppgaveForBehandlingen(behandlingId: UUID, oppgaveId: Long?): Oppgave {
        val gsakOppgaveId = oppgaveId ?: oppgaveService.finnSisteOppgaveForBehandling(behandlingId).gsakOppgaveId

        return oppgaveService.hentOppgave(gsakOppgaveId)
    }

    private fun finnResultatBegrunnelse(hendelse: Hendelse, behandlingId: UUID): String? {
        return when (hendelse) {
            Hendelse.PÅBEGYNT, Hendelse.MOTTATT -> null
            else -> {
                val vedtak = vedtakService.hentVedtak(behandlingId)
                return when (vedtak.resultatType) {
                    ResultatType.INNVILGE -> vedtak.periodeBegrunnelse
                    ResultatType.AVSLÅ -> vedtak.avslåBegrunnelse
                    ResultatType.HENLEGGE -> error("Ikke implementert")
                }
            }
        }
    }

    private fun finnSøknadstidspunkt(fagsak: Fagsak,
                                     behandlingId: UUID): LocalDateTime {
        return when (fagsak.stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> søknadService.hentOvergangsstønad(behandlingId).datoMottatt
            Stønadstype.BARNETILSYN -> søknadService.hentBarnetilsyn(behandlingId).datoMottatt
            Stønadstype.SKOLEPENGER -> søknadService.hentSkolepenger(behandlingId).datoMottatt
        }
    }


    companion object {

        fun opprettTask(
                behandlingId: UUID,
                hendelse: Hendelse,
                hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
                gjeldendeSaksbehandler: String,
                oppgaveId: Long? = null
        ): Task =
                Task(
                        type = TYPE,
                        payload = objectMapper.writeValueAsString(
                                BehandlingsstatistikkTaskPayload(
                                        behandlingId,
                                        hendelse,
                                        hendelseTidspunkt,
                                        gjeldendeSaksbehandler,
                                        oppgaveId
                                )
                        ),
                        properties = Properties().apply {
                            this["saksbehandler"] = gjeldendeSaksbehandler
                            this["behandlingId"] = behandlingId.toString()
                            this["hendelse"] = hendelse.name
                            this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                            this["oppgaveId"] = oppgaveId.toString() ?: ""
                        })


        const val TYPE = "behandlingsstatistikkTask"
    }

}

data class BehandlingsstatistikkTaskPayload(
        val behandlingId: UUID,
        val hendelse: Hendelse,
        val hendelseTidspunkt: LocalDateTime,
        val gjeldendeSaksbehandler: String,
        val oppgaveId: Long?
)