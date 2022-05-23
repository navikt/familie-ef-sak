package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.nyesteGrunnbeløpGyldigFraOgMed
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GOmregningTaskService(val fagsakRepository: FagsakRepository,
                            val featureToggleService: FeatureToggleService,
                            val gOmregningTask: GOmregningTask) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 45 12 23 5 ?")
    fun opprettGOmregningTaskForBehandlingerMedUtdatertG(): Int {

        feilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.omberegning")) {
            "Feature toggle for omberegning er disabled"
        }
        logger.info("Starter opprettelse av tasker for G-omregning.")
        val fagsakIder = fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(nyesteGrunnbeløpGyldigFraOgMed)
        fagsakIder.forEach {
            gOmregningTask.opprettTask(it)
        }
        logger.info("Opprettet ${fagsakIder.size} tasker for G-omregning.")
        return fagsakIder.size
    }

}