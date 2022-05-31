package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Properties
import java.util.UUID

@Unprotected // kommer kunne brukes uten token
@RestController
@RequestMapping("api/g-omregning-opprydding")
class GOmregningOppryddingController(
    private val gOmregningOppryddingService: GOmregningOppryddingService,
    private val gOmregningOppryddingTask: GOmregningOppryddingTask
) {

    @PostMapping("{fagsakId}")
    fun migrerAutomatiskt(@PathVariable("fagsakId") fagsakId: UUID) {
        gOmregningOppryddingService.ryddOppGOmregninger(fagsakId)
    }

    @PostMapping()
    fun opprettJobb() {
        gOmregningOppryddingTask.opprettTasks()
    }
}

@Service
@TaskStepBeskrivelse(
    taskStepType = GOmregningOppryddingTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "G-omregning opprydding"
)
class GOmregningOppryddingTask(
    private val gOmregningOppryddingService: GOmregningOppryddingService,
    private val behandlingRepository: BehandlingRepository,
    private val taskRepository: TaskRepository
) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandling = behandlingRepository.finnSaksbehandling(behandlingId)
        gOmregningOppryddingService.ryddOppGOmregninger(saksbehandling)
    }

    @Transactional
    fun opprettTasks() {
        val behandlingIder = behandlingRepository.finnGOmregninger()
        behandlingIder.map { behandlingId ->
            val properties = Properties().apply {
                setProperty("behandlingId", behandlingId.toString())
                setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
            }
            Task(TYPE, behandlingId.toString()).copy(metadataWrapper = PropertiesWrapper(properties))
        }.let {
            taskRepository.saveAll(it)
        }
    }

    companion object {

        const val TYPE = "G-omregning-opprydding"
    }
}

@Service
class GOmregningOppryddingService(
    private val søknadService: SøknadService,
    private val barnService: BarnService,
    private val vurderingService: VurderingService,
    private val vurderingRepository: VilkårsvurderingRepository,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val behandlingService: BehandlingService,
    private val behandlingRepository: BehandlingRepository
) {

    @Transactional
    fun ryddOppGOmregninger(fagsakId: UUID) {
        val behandling = behandlingRepository.findByFagsakIdAndÅrsak(fagsakId, BehandlingÅrsak.G_OMREGNING)
        ryddOppGOmregninger(behandlingService.hentSaksbehandling(behandling.id))
    }

    @Transactional
    fun ryddOppGOmregninger(saksbehandling: Saksbehandling) {
        feilHvis(saksbehandling.årsak != BehandlingÅrsak.G_OMREGNING) {
            "Behandling er ikke g-omregning"
        }
        feilHvis(saksbehandling.opprettetAv != SYSTEM_FORKORTELSE) {
            "Behandling er ikke maskinellt opprettet"
        }
        val behandlingId = saksbehandling.id
        val forrigeBehandlingId = saksbehandling.forrigeBehandlingId
            ?: error("Finner ikke forrigeBehandlingId til $behandlingId")
        søknadService.kopierSøknad(forrigeBehandlingId, behandlingId)

        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        barnService.opprettBarnForRevurdering(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId,
            nyeBarnPåRevurdering = emptyList(),
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = saksbehandling.stønadstype
        )

        val (_, metadata) = vurderingService.hentGrunnlagOgMetadata(behandlingId)
        vurderingRepository.deleteAllByBehandlingId(behandlingId)
        vurderingService.kopierVurderingerTilNyBehandling(
            forrigeBehandlingId,
            behandlingId,
            metadata,
            saksbehandling.stønadstype
        )
    }
}