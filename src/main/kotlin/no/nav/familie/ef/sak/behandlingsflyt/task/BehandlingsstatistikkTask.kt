package no.nav.familie.ef.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingMetode
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsstatistikkDto
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.ef.iverksett.ÅrsakRevurderingDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.ef.StønadType.SKOLEPENGER
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
    beskrivelse = "Sender behandlingsstatistikk til iverksett",
)
class BehandlingsstatistikkTask(
    private val iverksettClient: IverksettClient,
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val vedtakRepository: VedtakRepository,
    private val oppgaveService: OppgaveService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val årsakRevurderingService: ÅrsakRevurderingService,
) : AsyncTaskStep {
    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId, behandlingMetode) =
            objectMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)

        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val årsakRevurdering = årsakRevurderingService.hentÅrsakRevurdering(behandlingId)

        val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)

        val resultatBegrunnelse = finnResultatBegrunnelse(hendelse, vedtak, saksbehandling)
        val søker = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlagsdata.søker
        val henvendelseTidspunkt = finnHenvendelsestidspunkt(saksbehandling)
        val relatertEksternBehandlingId =
            saksbehandling.forrigeBehandlingId?.let { behandlingService.hentBehandling(it).eksternId }
        val erAutomatiskGOmregning = saksbehandling.årsak == BehandlingÅrsak.G_OMREGNING && saksbehandling.opprettetAv == "VL"

        val behandlingsstatistikkDto =
            BehandlingsstatistikkDto(
                behandlingId = behandlingId,
                eksternBehandlingId = saksbehandling.eksternId,
                personIdent = saksbehandling.ident,
                gjeldendeSaksbehandlerId =
                    if (erAutomatiskGOmregning) {
                        "VL"
                    } else {
                        finnSaksbehandler(hendelse, vedtak, gjeldendeSaksbehandler)
                    },
                beslutterId =
                    if (hendelse.erBesluttetEllerFerdig()) {
                        vedtak?.beslutterIdent
                    } else {
                        null
                    },
                eksternFagsakId = saksbehandling.eksternFagsakId,
                hendelseTidspunkt = hendelseTidspunkt.atZone(zoneIdOslo),
                behandlingOpprettetTidspunkt = saksbehandling.opprettetTid.atZone(zoneIdOslo),
                hendelse = hendelse,
                behandlingResultat = saksbehandling.resultat.name,
                resultatBegrunnelse = resultatBegrunnelse,
                opprettetEnhet = sisteOppgaveForBehandling?.opprettetAvEnhetsnr ?: MASKINELL_JOURNALFOERENDE_ENHET,
                ansvarligEnhet = sisteOppgaveForBehandling?.tildeltEnhetsnr ?: MASKINELL_JOURNALFOERENDE_ENHET,
                strengtFortroligAdresse = søker.adressebeskyttelse?.erStrengtFortrolig() ?: false,
                stønadstype = saksbehandling.stønadstype,
                behandlingstype = BehandlingType.valueOf(saksbehandling.type.name),
                henvendelseTidspunkt = henvendelseTidspunkt.atZone(zoneIdOslo),
                relatertEksternBehandlingId = relatertEksternBehandlingId,
                relatertBehandlingId = null,
                behandlingMetode = behandlingMetode,
                behandlingÅrsak = saksbehandling.årsak,
                kravMottatt = saksbehandling.kravMottatt,
                årsakRevurdering =
                    årsakRevurdering?.let {
                        ÅrsakRevurderingDto(it.opplysningskilde, it.årsak)
                    },
                avslagÅrsak = vedtak?.avslåÅrsak,
                kategori = saksbehandling.kategori,
            )

        iverksettClient.sendBehandlingsstatistikk(behandlingsstatistikkDto)
    }

    private fun finnSisteOppgaveForBehandlingen(
        behandlingId: UUID,
        oppgaveId: Long?,
    ): Oppgave? {
        val gsakOppgaveId = oppgaveId ?: oppgaveService.finnSisteOppgaveForBehandling(behandlingId)?.gsakOppgaveId

        return gsakOppgaveId?.let { oppgaveService.hentOppgave(it) }
    }

    private fun Hendelse.erBesluttetEllerFerdig() = this.name == Hendelse.BESLUTTET.name || this.name == Hendelse.FERDIG.name

    private fun finnResultatBegrunnelse(
        hendelse: Hendelse,
        vedtak: Vedtak?,
        saksbehandling: Saksbehandling,
    ): String? {
        if (saksbehandling.resultat == BehandlingResultat.HENLAGT) {
            return saksbehandling.henlagtÅrsak?.name ?: error("Mangler henlagtårsak for henlagt behandling")
        }
        return when (hendelse) {
            Hendelse.PÅBEGYNT, Hendelse.MOTTATT, Hendelse.VENTER -> null
            Hendelse.VEDTATT, Hendelse.BESLUTTET, Hendelse.HENLAGT, Hendelse.FERDIG -> {
                return when (vedtak?.resultatType) {
                    ResultatType.INNVILGE, ResultatType.INNVILGE_UTEN_UTBETALING ->
                        utledBegrunnelseForInnvilgetVedtak(
                            saksbehandling.stønadstype,
                            vedtak,
                        )
                    ResultatType.AVSLÅ, ResultatType.OPPHØRT -> vedtak.avslåBegrunnelse
                    ResultatType.HENLEGGE -> error("ResultatType henlegge er ikke i bruk for vedtak")
                    ResultatType.SANKSJONERE -> vedtak.internBegrunnelse
                    null -> error("Mangler vedtak")
                }
            }
        }
    }

    private fun utledBegrunnelseForInnvilgetVedtak(
        stønadType: StønadType,
        vedtak: Vedtak,
    ) = when (stønadType) {
        OVERGANGSSTØNAD -> vedtak.periodeBegrunnelse
        BARNETILSYN -> vedtak.barnetilsyn?.begrunnelse
        SKOLEPENGER -> vedtak.skolepenger?.begrunnelse
    }

    private fun finnSaksbehandler(
        hendelse: Hendelse,
        vedtak: Vedtak?,
        gjeldendeSaksbehandler: String?,
    ): String =
        when (hendelse) {
            Hendelse.MOTTATT, Hendelse.PÅBEGYNT, Hendelse.VENTER, Hendelse.HENLAGT ->
                gjeldendeSaksbehandler ?: error("Mangler saksbehandler for hendelse")
            Hendelse.VEDTATT, Hendelse.BESLUTTET, Hendelse.FERDIG ->
                vedtak?.saksbehandlerIdent ?: gjeldendeSaksbehandler ?: error("Mangler saksbehandler på vedtaket")
        }

    private fun finnHenvendelsestidspunkt(saksbehandling: Saksbehandling): LocalDateTime =
        when (saksbehandling.type) {
            FØRSTEGANGSBEHANDLING -> søknadService.finnDatoMottattForSøknad(saksbehandling.id) ?: saksbehandling.opprettetTid
            REVURDERING -> saksbehandling.opprettetTid
        }

    companion object {
        fun opprettMottattTask(
            behandlingId: UUID,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            saksbehandler: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            oppgaveId: Long?,
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.MOTTATT,
                hendelseTidspunkt = hendelseTidspunkt,
                gjeldendeSaksbehandler = saksbehandler,
                oppgaveId = oppgaveId,
            )

        fun opprettPåbegyntTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.PÅBEGYNT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )

        fun opprettVenterTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VENTER,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(),
            )

        fun opprettVedtattTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VEDTATT,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettBesluttetTask(
            behandlingId: UUID,
            oppgaveId: Long?,
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.BESLUTTET,
                hendelseTidspunkt = LocalDateTime.now(),
                oppgaveId = oppgaveId,
            )

        fun opprettFerdigTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.FERDIG,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettFerdigTask(
            behandlingId: UUID,
            hendelseTidspunkt: LocalDateTime,
            gjeldendeSaksbehandler: String,
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.FERDIG,
                hendelseTidspunkt = hendelseTidspunkt,
                gjeldendeSaksbehandler = gjeldendeSaksbehandler,
            )

        private fun opprettTask(
            behandlingId: UUID,
            hendelse: Hendelse,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String? = null,
            oppgaveId: Long? = null,
            behandlingMetode: BehandlingMetode? = null,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        BehandlingsstatistikkTaskPayload(
                            behandlingId,
                            hendelse,
                            hendelseTidspunkt,
                            gjeldendeSaksbehandler,
                            oppgaveId,
                            behandlingMetode,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = gjeldendeSaksbehandler ?: ""
                        this["behandlingId"] = behandlingId.toString()
                        this["hendelse"] = hendelse.name
                        this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                        this["oppgaveId"] = oppgaveId?.toString() ?: ""
                    },
            )

        const val TYPE = "behandlingsstatistikkTask"
    }
}

data class BehandlingsstatistikkTaskPayload(
    val behandlingId: UUID,
    val hendelse: Hendelse,
    val hendelseTidspunkt: LocalDateTime,
    val gjeldendeSaksbehandler: String?,
    val oppgaveId: Long?,
    val behandlingMetode: BehandlingMetode?,
)
