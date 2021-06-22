package no.nav.familie.ef.sak.task

import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDateTime
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.iverksett.IverksettClient
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
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.UUID

@Service
@TaskStepBeskrivelse(
        taskStepType = BehandlingsstatistikkTask.TYPE,
        beskrivelse = "Sender behandlingsstatistikk til iverksett"
)

class BehandlingsstatistikkTask(private val iverksettClient: IverksettClient,
                                private val behandlingService: BehandlingService,
                                private val fagsakService: FagsakService,
                                val søknadService: SøknadService,
                                val vedtakService: VedtakService,
                                val oppgaveService: OppgaveService,
                                val personService: PersonService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler) = objectMapper.readValue<BehandlingsstatistikkTaskPayload>(
                task.payload
        )

        val personIdent = behandlingService.hentAktivIdent(behandlingId)

        val behandling = behandlingService.hentBehandling(behandlingId)

        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)


        val søknadstidspunkt = when (fagsak.stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> søknadService.hentOvergangsstønad(behandlingId).datoMottatt
            Stønadstype.BARNETILSYN -> søknadService.hentBarnetilsyn(behandlingId).datoMottatt
            Stønadstype.SKOLEPENGER -> søknadService.hentSkolepenger(behandlingId).datoMottatt
        }

        val vedtak = vedtakService.hentVedtak(behandlingId)

        val gsakOppgaveId = oppgaveService.finnSisteOppgaveForBehandling(behandlingId).gsakOppgaveId

        val sisteOppgaveForBehandling = oppgaveService.hentOppgave(gsakOppgaveId)

        val resultatBegrunnelse = when (vedtak.resultatType) {
            ResultatType.INNVILGE -> vedtak.periodeBegrunnelse
            ResultatType.AVSLÅ -> vedtak.avslåBegrunnelse
            ResultatType.HENLEGGE -> error("Ikke implementert")
        }

        val søker = personService.hentSøker(personIdent);

        val behandlingsstatistikkDto = BehandlingsstatistikkDto(
                behandlingId = behandlingId,
                personIdent = personIdent,
                gjeldendeSaksbehandlerId = gjeldendeSaksbehandler,
                eksternFagsakId = fagsak.eksternId.id.toString(),
                hendelseTidspunkt = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                søknadstidspunkt = søknadstidspunkt.atZone(ZoneId.of("Europe/Oslo")),
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


    // TODO:  opprett Task(type=behandlingsstatistikk, behandlingId, gjeldendeTidspunkt, hendelse=mottatt, gjeldendeSaksbehandler)
    companion object {

        fun opprettTask(
                behandlingId: UUID,
                hendelse: Hendelse,
                hendelseTidspunkt: LocalDateTime,
                gjeldendeSaksbehandler: String,
                oppgaveId: String
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
                        )
                )

        const val TYPE = "behandlingsstatistikkTask"
    }

}

data class BehandlingsstatistikkTaskPayload(
        val behandlingId: UUID,
        val hendelse: Hendelse,
        val hendelseTidspunkt: LocalDateTime,
        val gjeldendeSaksbehandler: String,
        val oppgaveId: String
)