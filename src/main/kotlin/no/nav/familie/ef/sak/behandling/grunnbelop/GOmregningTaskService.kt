package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggle
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.UUID

@Profile("!integrasjonstest")
@Service
class GOmregningTaskServiceScheduler(
    private val gOmregningTaskService: GOmregningTaskService,
    private val featureToggleService: FeatureToggleService,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${G_OMREGNING_CRON_EXPRESSION}")
    fun opprettGOmregningTaskForBehandlingerMedUtdatertG() {
        if (featureToggleService.isEnabled(FeatureToggle.GBeregningScheduler)) {
            gOmregningTaskService.opprettGOmregningTaskForBehandlingerMedUtdatertG()
        }
    }
}

@Service
class GOmregningTaskService(
    private val fagsakRepository: FagsakRepository,
    private val gOmregningTask: GOmregningTask,
    private val featureToggleService: FeatureToggleService,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun opprettGOmregningTaskForBehandlingerMedUtdatertG(): Int {
        logger.info("Starter opprettelse av tasker for G-omregning.")
        val fagsakIder = finnFagsakIder()
        logger.info("Funnet ${fagsakIder.size} fagsaker aktuelle for G-omregning.")
        try {
            var counter = 0
            fagsakIder.forEach {
                if (gOmregningTask.opprettTask(it)) {
                    counter++
                }
            }
            logger.info("Opprettet $counter tasker for G-omregning.")
        } catch (e: DbActionExecutionException) {
            if (e.cause is DuplicateKeyException) {
                logger.info("To podder har forsøkt å starte G-omregning samtidig. Stopper den ene.")
                return 0
            } else {
                throw e
            }
        }
        return fagsakIder.size
    }

    private fun finnFagsakIder(): List<UUID> =
        fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(
            nyesteGrunnbeløpGyldigFraOgMed.atDay(1),
        )
}
