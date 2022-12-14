package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.FørstegangsbehandlingDto
import no.nav.familie.ef.sak.behandling.migrering.InfotrygdPeriodeValideringService
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FørstegangsbehandlingService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val barnService: BarnService,
    private val taskService: TaskService,
    private val oppgaveService: OppgaveService,
    private val iverksettService: IverksettService,
    private val infotrygdPeriodeValideringService: InfotrygdPeriodeValideringService,
    private val featureToggleService: FeatureToggleService

) {

    @Transactional
    fun opprettFørstegangsbehandling(fagsakId: UUID, førstegangsBehandlingRequest: FørstegangsbehandlingDto): Behandling {
        feilHvisIkke(featureToggleService.isEnabled(Toggle.FØRSTEGANGSBEHANDLING)) {
            "Opprettelse av førstegangsbehandling er skrudd av"
        }
        validerGyldigÅrsak(førstegangsBehandlingRequest.behandlingsårsak)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(fagsakId)
        infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(fagsak)
        val behandling = opprettBehandling(fagsakId, førstegangsBehandlingRequest)
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(behandling.id)

        leggTilBarn(behandling, fagsak, førstegangsBehandlingRequest, grunnlagsdata)

        val oppgaveId = opprettOppgave(behandling) // TODO: Gjør denne asynk når Johan har merget sin branch
        iverksettService.startBehandling(behandling, fagsak)
        lagStatistikkTasks(behandling, oppgaveId)
        return behandling
    }

    private fun lagStatistikkTasks(behandling: Behandling, oppgaveId: Long) {
        taskService.save(
            BehandlingsstatistikkTask.opprettMottattTask(behandlingId = behandling.id, oppgaveId = oppgaveId)
        )
    }

    private fun opprettOppgave(behandling: Behandling): Long {
        val oppgaveId = oppgaveService.opprettOppgave(
            behandlingId = behandling.id,
            oppgavetype = Oppgavetype.BehandleSak,
            tilordnetNavIdent = SikkerhetContext.hentSaksbehandler(true),
            beskrivelse = "Førstegangsbehandling - manuelt opprettet"
        )
        return oppgaveId
    }

    private fun opprettBehandling(
        fagsakId: UUID,
        førstegangsBehandlingRequest: FørstegangsbehandlingDto
    ): Behandling {
        val behandling = behandlingService.opprettBehandling(
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            fagsakId = fagsakId,
            behandlingsårsak = førstegangsBehandlingRequest.behandlingsårsak,
            kravMottatt = førstegangsBehandlingRequest.kravMottatt
        )
        return behandling
    }

    private fun leggTilBarn(
        behandling: Behandling,
        fagsak: Fagsak,
        førstegangsBehandlingRequest: FørstegangsbehandlingDto,
        grunnlagsdata: GrunnlagsdataMedMetadata
    ) {
        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId = behandling.id,
            fagsakId = fagsak.id,
            stønadstype = fagsak.stønadstype,
            ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.PAPIRSØKNAD,
            barnSomSkalFødes = førstegangsBehandlingRequest.barn,
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE
        )
    }

    private fun validerGyldigÅrsak(behandlingsårsak: BehandlingÅrsak) {
        feilHvisIkke(behandlingsårsak == BehandlingÅrsak.PAPIRSØKNAD || behandlingsårsak == BehandlingÅrsak.NYE_OPPLYSNINGER) {
            "Kan ikke opprette førstegangsbehandlinge med behandlingsårsak $behandlingsårsak"
        }
    }
}
