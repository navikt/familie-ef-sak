package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnBehandlingerMedGammelGTask.TYPE,
    beskrivelse = "Månedlig jobb som finner behandlinger som ikke har blitt g-omregnet",
)
class FinnBehandlingerMedGammelGTask(
    val behandlingRepository: BehandlingRepository,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        logger.info("Starter jobb som finner behandlinger som ikke har blitt g-omregnet")
        val fagsakerTilManuellBehandling = behandlingRepository.finnFerdigstilteBehandlingerMedUtdatertGBelopSomMåBehandlesManuelt(Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fomDato)
        feilHvis(fagsakerTilManuellBehandling.size > 0) { "Ferdigstilte behandlinger med utdatert G " }
    }

    companion object {
        const val TYPE = "finnBehandlingerMedGammelGTask"
    }
}
