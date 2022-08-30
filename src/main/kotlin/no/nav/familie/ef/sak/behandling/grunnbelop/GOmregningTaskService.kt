package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.nyesteGrunnbeløpGyldigFraOgMed
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GOmregningTaskService(
    private val fagsakRepository: FagsakRepository,
    private val gOmregningTask: GOmregningTask
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${G_OMREGNING_CRON_EXPRESSION}")
    fun opprettGOmregningTaskForBehandlingerMedUtdatertG(): Int {
        logger.info("Starter opprettelse av tasker for G-omregning.")
        val fagsakIder = fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(nyesteGrunnbeløpGyldigFraOgMed.atDay(1))
        try {
            fagsakIder.forEach {
                gOmregningTask.opprettTask(it)
            }
            logger.info("Opprettet ${fagsakIder.size} tasker for G-omregning.")
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
}
