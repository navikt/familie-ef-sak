package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.FørstegangsbehandlingDto
import no.nav.familie.ef.sak.behandling.migrering.InfotrygdPeriodeValideringService
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
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
    private val infotrygdPeriodeValideringService: InfotrygdPeriodeValideringService,
) {
    @Transactional
    fun opprettFørstegangsbehandling(
        fagsakId: UUID,
        førstegangsBehandlingRequest: FørstegangsbehandlingDto,
    ): Behandling {
        validerGyldigÅrsak(førstegangsBehandlingRequest.behandlingsårsak)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(fagsakId)
        infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(fagsak)
        val behandling = opprettBehandling(fagsakId, førstegangsBehandlingRequest)
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(behandling.id)

        leggTilBarn(behandling, fagsak, førstegangsBehandlingRequest, grunnlagsdata)

        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    saksbehandler = SikkerhetContext.hentSaksbehandler(),
                    beskrivelse = "Førstegangsbehandling - manuelt opprettet",
                ),
            ),
        )
        return behandling
    }

    private fun opprettBehandling(
        fagsakId: UUID,
        førstegangsBehandlingRequest: FørstegangsbehandlingDto,
    ): Behandling {
        val behandling =
            behandlingService.opprettBehandling(
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsakId = fagsakId,
                behandlingsårsak = førstegangsBehandlingRequest.behandlingsårsak,
                kravMottatt = førstegangsBehandlingRequest.kravMottatt,
            )
        return behandling
    }

    private fun leggTilBarn(
        behandling: Behandling,
        fagsak: Fagsak,
        førstegangsBehandlingRequest: FørstegangsbehandlingDto,
        grunnlagsdata: GrunnlagsdataMedMetadata,
    ) {
        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId = behandling.id,
            fagsakId = fagsak.id,
            stønadstype = fagsak.stønadstype,
            ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.PAPIRSØKNAD,
            barnSomSkalFødes = førstegangsBehandlingRequest.barn,
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
        )
    }

    private fun validerGyldigÅrsak(behandlingsårsak: BehandlingÅrsak) {
        feilHvisIkke(behandlingsårsak == BehandlingÅrsak.PAPIRSØKNAD || behandlingsårsak == BehandlingÅrsak.NYE_OPPLYSNINGER || behandlingsårsak == BehandlingÅrsak.MANUELT_OPPRETTET) {
            "Kan ikke opprette førstegangsbehandlinge med behandlingsårsak $behandlingsårsak"
        }
    }
}
