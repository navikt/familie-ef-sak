package no.nav.familie.ef.sak.metrics

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ef.sak.metrics.domain.MålerRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger

@Service
class MålerService(
    private val målerRepository: MålerRepository,
) {
    private val åpneBehandlingerPerUkeGauge = MultiGauge.builder("KlarTilBehandlingPerUke").register(Metrics.globalRegistry)
    private val åpneBehandlingerGauge = MultiGauge.builder("KlarTilBehandling").register(Metrics.globalRegistry)
    private val løpendeBehandlingerGauge = MultiGauge.builder("LopendeBehandlinger").register(Metrics.globalRegistry)
    private val løpendeBehandlingerBeløpGauge = MultiGauge.builder("LopendeBehandlingerBelop").register(Metrics.globalRegistry)
    private val vedtakGauge = MultiGauge.builder("Vedtak").register(Metrics.globalRegistry)
    private val antallMigreringerGauge = Metrics.gauge("AntallMigreringer", AtomicInteger()) ?: error("Forventer not null")
    private val antallSanksjonerGauge = Metrics.gauge("AntallSanksjoner", AtomicInteger()) ?: error("Forventer not null")

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun antallMigreringer() {
        val antallBehandlinger = målerRepository.finnAntallBehandlingerAvÅrsak(BehandlingÅrsak.MIGRERING)
        logger.info("Antall migreringer=$antallBehandlinger")
        antallMigreringerGauge.set(antallBehandlinger)
    }

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun antallLøpendeSaker() {
        val now = YearMonth.now()
        val løpendeSaker =
            målerRepository.finnAntallLøpendeSaker(
                now.minusMonths(2).atDay(1),
                now.plusMonths(2).atDay(1),
            )

        løpendeBehandlingerGauge.register(
            løpendeSaker.map {
                MultiGauge.Row.of(
                    Tags.of(
                        "ytelse",
                        it.stonadstype.name,
                        "maned",
                        it.dato.year.toString() +
                            "-" +
                            it.dato.monthValue
                                .toString()
                                .padStart(2, '0'),
                    ),
                    it.antall,
                )
            },
            true,
        )

        løpendeBehandlingerBeløpGauge.register(
            løpendeSaker.map {
                MultiGauge.Row.of(
                    Tags.of(
                        "ytelse",
                        it.stonadstype.name,
                        "maned",
                        it.dato.year.toString() +
                            "-" +
                            it.dato.monthValue
                                .toString()
                                .padStart(2, '0'),
                    ),
                    it.belop,
                )
            },
            true,
        )
    }

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun antallSanksjoner() {
        val antallSanksjoner = målerRepository.finnAntallSanksjoner()
        logger.info("Antall sanksjoner=$antallSanksjoner")
        antallSanksjonerGauge.set(antallSanksjoner)
    }

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlingerPerUke() {
        val behandlinger = målerRepository.finnÅpneBehandlingerPerUke()
        logger.info("Åpne behandlinger per uke returnerte ${behandlinger.sumOf { it.antall }} fordelt på ${behandlinger.size} uker.")
        val rows =
            behandlinger.map {
                MultiGauge.Row.of(
                    Tags.of(
                        "ytelse",
                        it.stonadstype.name,
                        "uke",
                        it.år.toString() + "-" + it.uke.toString().padStart(2, '0'),
                    ),
                    it.antall,
                )
            }

        åpneBehandlingerPerUkeGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 90 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlinger() {
        val behandlinger = målerRepository.finnÅpneBehandlinger()
        logger.info(
            "Åpne behandlinger returnerte ${behandlinger.sumOf { it.antall }} " +
                "fordelt på ${behandlinger.size} statuser.",
        )
        val rows =
            behandlinger.map {
                MultiGauge.Row.of(
                    Tags.of(
                        "ytelse",
                        it.stonadstype.name,
                        "status",
                        it.status.name,
                    ),
                    it.antall,
                )
            }

        åpneBehandlingerGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 180 * 1000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun vedtakPerUke() {
        val data = målerRepository.finnVedtakPerUke()
        logger.info("Vedtak returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} typer/uker.")

        val rows =
            data.map {
                MultiGauge.Row.of(
                    Tags.of(
                        "ytelse",
                        it.stonadstype.name,
                        "resultat",
                        it.resultat.name,
                        "arsak",
                        it.arsak.name,
                        "henlagtarsak",
                        it.henlagt_arsak?.name ?: "",
                        "uke",
                        it.år.toString() + "-" + it.uke.toString().padStart(2, '0'),
                    ),
                    it.antall,
                )
            }
        vedtakGauge.register(rows)
    }

    companion object {
        const val OPPDATERINGSFREKVENS = 30 * 60 * 1000L
    }
}
