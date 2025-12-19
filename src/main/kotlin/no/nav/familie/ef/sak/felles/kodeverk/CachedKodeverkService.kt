package no.nav.familie.ef.sak.felles.kodeverk

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheManager = "kodeverkCache")
class CachedKodeverkService(
    private val kodeverkClient: KodeverkClient,
) {
    @Cacheable("kodeverk_landkoder")
    fun hentLandkoder(): KodeverkDto = kodeverkClient.hentKodeverkLandkoder()

    @Cacheable("kodeverk_poststed")
    fun hentPoststed(): KodeverkDto = kodeverkClient.hentKodeverkPoststed()

    @Cacheable("kodeverk_inntekt")
    fun hentInntekt(): InntektKodeverkDto = kodeverkClient.hentKodeverkInntekt()
}

@Profile("!integrasjonstest")
@Component
class KodeverkInitializer(
    private val cachedKodeverkService: CachedKodeverkService,
) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = Logg.getLogger(this::class)

    @Scheduled(cron = "0 0 2 * * *")
    fun syncKodeverk() {
        logger.info("Kjører schedulert jobb for å hente kodeverk")
        sync()
    }

    override fun onApplicationEvent(p0: ApplicationReadyEvent) {
        sync()
    }

    private fun sync() {
        syncKodeverk("Landkoder", cachedKodeverkService::hentLandkoder)
        syncKodeverk("Poststed", cachedKodeverkService::hentPoststed)
        syncKodeverk("Inntekt", cachedKodeverkService::hentInntekt)
    }

    private fun syncKodeverk(
        navn: String,
        henter: () -> Unit,
    ) {
        try {
            logger.info("Henter $navn")
            henter.invoke()
        } catch (e: Exception) {
            logger.warn("Feilet henting av $navn ${e.message}")
        }
    }
}
