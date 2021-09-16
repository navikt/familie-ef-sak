package no.nav.familie.ef.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.repository.domain.BehandlingType.REVURDERING
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.GrunnlagsdataService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsstatistikkDto
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
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
                                private val vedtakRepository: VedtakRepository,
                                private val oppgaveService: OppgaveService,
                                private val grunnlagsdataService: GrunnlagsdataService
) : AsyncTaskStep {

    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId) =
                objectMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)

        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val personIdent = fagsak.hentAktivIdent()

        val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)

        val resultatBegrunnelse = finnResultatBegrunnelse(hendelse, vedtak)
        val søker = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlagsdata.søker
        val henvendelseTidspunkt = finnHenvendelsestidspunkt(behandling)

        val behandlingsstatistikkDto = BehandlingsstatistikkDto(
                behandlingId = behandlingId,
                personIdent = personIdent,
                gjeldendeSaksbehandlerId = finnSaksbehandler(hendelse, vedtak, gjeldendeSaksbehandler),
                eksternFagsakId = fagsak.eksternId.id.toString(),
                hendelseTidspunkt = hendelseTidspunkt.atZone(zoneIdOslo),
                søknadstidspunkt = henvendelseTidspunkt.atZone(zoneIdOslo),
                hendelse = hendelse,
                behandlingResultat = behandling.resultat.name,
                resultatBegrunnelse = resultatBegrunnelse,
                opprettetEnhet = sisteOppgaveForBehandling.opprettetAvEnhetsnr ?: "9999",
                ansvarligEnhet = sisteOppgaveForBehandling.tildeltEnhetsnr ?: "9999",
                strengtFortroligAdresse = søker.adressebeskyttelse?.erStrengtFortrolig() ?: false,
                stønadstype = StønadType.valueOf(fagsak.stønadstype.name),
                behandlingstype = BehandlingType.valueOf(behandling.type.name),
                henvendelseTidspunkt = henvendelseTidspunkt.atZone(zoneIdOslo),
                relatertBehandlingId = behandling.forrigeBehandlingId
        )

        iverksettClient.sendBehandlingsstatistikk(behandlingsstatistikkDto)
    }

    private fun finnSisteOppgaveForBehandlingen(behandlingId: UUID, oppgaveId: Long?): Oppgave {
        val gsakOppgaveId = oppgaveId ?: oppgaveService.finnSisteOppgaveForBehandling(behandlingId).gsakOppgaveId

        return oppgaveService.hentOppgave(gsakOppgaveId)
    }

    private fun finnResultatBegrunnelse(hendelse: Hendelse, vedtak: Vedtak?): String? {
        return when (hendelse) {
            Hendelse.PÅBEGYNT, Hendelse.MOTTATT -> null
            else -> {
                return when (vedtak?.resultatType) {
                    ResultatType.INNVILGE -> vedtak.periodeBegrunnelse
                    ResultatType.AVSLÅ -> vedtak.avslåBegrunnelse
                    ResultatType.HENLEGGE -> error("Ikke implementert")
                    else -> error("Mangler vedtak")
                }
            }
        }
    }

    private fun finnSaksbehandler(hendelse: Hendelse, vedtak: Vedtak?, gjeldendeSaksbehandler: String?): String {
        return when (hendelse) {
            Hendelse.MOTTATT, Hendelse.PÅBEGYNT -> gjeldendeSaksbehandler ?: error("Mangler saksbehandler for hendelse")
            Hendelse.VEDTATT -> vedtak?.saksbehandlerIdent ?: error("Mangler saksbehandler på vedtaket")
            Hendelse.BESLUTTET, Hendelse.FERDIG -> vedtak?.beslutterIdent ?: error("Mangler beslutter på vedtaket")
        }
    }

    private fun finnHenvendelsestidspunkt(behandling: Behandling): LocalDateTime {
        return when (behandling.type) {
            FØRSTEGANGSBEHANDLING -> søknadService.finnDatoMottattForSøknad(behandling.id)
            REVURDERING -> LocalDateTime.now()
            else -> error("Støtter ikke uthenting av mottatt-dato for ${behandling.type}")
        }
    }


    companion object {

        fun opprettMottattTask(behandlingId: UUID, oppgaveId: Long): Task =
                opprettTask(behandlingId = behandlingId,
                            hendelse = Hendelse.MOTTATT,
                            hendelseTidspunkt = LocalDateTime.now(),
                            gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true),
                            oppgaveId = oppgaveId)

        fun opprettPåbegyntTask(behandlingId: UUID): Task =
                opprettTask(behandlingId = behandlingId,
                            hendelse = Hendelse.PÅBEGYNT,
                            hendelseTidspunkt = LocalDateTime.now(),
                            gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true))

        fun opprettVedtattTask(behandlingId: UUID,
                               hendelseTidspunkt: LocalDateTime,
                               oppgaveId: Long?): Task =
                opprettTask(behandlingId = behandlingId,
                            hendelse = Hendelse.VEDTATT,
                            hendelseTidspunkt = hendelseTidspunkt,
                            oppgaveId = oppgaveId)

        fun opprettBesluttetTask(behandlingId: UUID,
                                 oppgaveId: Long?): Task =
                opprettTask(behandlingId = behandlingId,
                            hendelse = Hendelse.BESLUTTET,
                            hendelseTidspunkt = LocalDateTime.now(),
                            oppgaveId = oppgaveId)

        fun opprettFerdigTask(behandlingId: UUID): Task =
                opprettTask(behandlingId = behandlingId,
                            hendelse = Hendelse.FERDIG,
                            hendelseTidspunkt = LocalDateTime.now())

        private fun opprettTask(
                behandlingId: UUID,
                hendelse: Hendelse,
                hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
                gjeldendeSaksbehandler: String? = null,
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
                            this["saksbehandler"] = gjeldendeSaksbehandler ?: ""
                            this["behandlingId"] = behandlingId.toString()
                            this["hendelse"] = hendelse.name
                            this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                            this["oppgaveId"] = oppgaveId?.toString() ?: ""
                        })


        const val TYPE = "behandlingsstatistikkTask"
    }

}

data class BehandlingsstatistikkTaskPayload(
        val behandlingId: UUID,
        val hendelse: Hendelse,
        val hendelseTidspunkt: LocalDateTime,
        val gjeldendeSaksbehandler: String?,
        val oppgaveId: Long?
)