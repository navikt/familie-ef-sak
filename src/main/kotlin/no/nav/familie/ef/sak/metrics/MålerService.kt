package no.nav.familie.ef.sak.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ef.sak.metrics.domain.MålerRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference

@Service
class MålerService(private val målerRepository: MålerRepository) {

    private val åpneBehandlingerPerUkeGauge = MultiGauge.builder("KlarTilBehandlingPerUke").register(Metrics.globalRegistry)
    private val åpneBehandlingerGauge = MultiGauge.builder("KlarTilBehandling").register(Metrics.globalRegistry)
    private val vedtakGauge = MultiGauge.builder("Vedtak").register(Metrics.globalRegistry)
    private val antallMigreringerTeller: AtomicReference<Double> = AtomicReference(0.0)
    private val antallMigreringerGauge = Gauge.builder("AntallMigreringer",
                                                       antallMigreringerTeller,
                                                       AtomicReference<Double>::get).register(Metrics.globalRegistry)


    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun antallMigreringer() {
        val antallBehandlinger = målerRepository.finnAntallBehandlingerAvÅrsak(BehandlingÅrsak.MIGRERING)
        logger.info("Antall migreringer forrige gang=${antallMigreringerGauge.value()}")
        logger.info("Antall migreringer=$antallBehandlinger")
        antallMigreringerTeller.set(antallBehandlinger.toDouble())
    }

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlingerPerUke() {
        val behandlinger = målerRepository.finnÅpneBehandlingerPerUke()
        logger.info("Åpne behandlinger per uke returnerte ${behandlinger.sumOf { it.antall }} fordelt på ${behandlinger.size} uker.")
        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.stonadstype.name,
                                      "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')),
                              it.antall)
        }

        åpneBehandlingerPerUkeGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 90 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlinger() {
        val behandlinger = målerRepository.finnÅpneBehandlinger()
        logger.info("Åpne behandlinger returnerte ${behandlinger.sumOf { it.antall }} " +
                    "fordelt på ${behandlinger.size} statuser.")
        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.stonadstype.name,
                                      "status", it.status.name),
                              it.antall)
        }

        åpneBehandlingerGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 180 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun vedtakPerUke() {
        val data = målerRepository.finnVedtakPerUke()
        logger.info("Vedtak returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} typer/uker.")

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.stonadstype.name,
                                      "resultat", it.resultat.name,
                                      "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')),
                              it.antall)
        }
        vedtakGauge.register(rows)
    }

    companion object {

        const val OPPDATERINGSFREKVENS = 30 * 60 * 1000L
    }


}