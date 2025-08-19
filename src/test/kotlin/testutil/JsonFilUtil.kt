package no.nav.familie.ef.sak.testutil

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.kontrakter.felles.objectMapper
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class JsonFilUtil {
    companion object {
        fun lesFil(name: String): String =
            this::class.java.classLoader
                .getResource(name)!!
                .readText(StandardCharsets.UTF_8)

        private val ymFormatter = DateTimeFormatter.ofPattern("uuuu-MM")

        fun oppdaterMåneder(
            json: String,
            windowMonths: Long = 6,
        ): String {
            // 1) Finn alle YearMonth-forekomster i JSON-tekstfelter
            val months = mutableListOf<YearMonth>()
            runCatching { objectMapper.readTree(json) }
                .onSuccess { collectYearMonths(it, months) }
                .onFailure { return json } // hvis ikke gyldig JSON, gjør ingenting

            if (months.isEmpty()) return json

            // 2) Forankre på største (nyeste) måned i malen
            val maxInTemplate = months.maxOrNull()!!
            val delta = ChronoUnit.MONTHS.between(maxInTemplate, YearMonth.now())

            if (delta == 0L) return json // allerede "i rute"

            // 3) Bestem hvilke måneder vi faktisk skal flytte (max, max-1, ..., max-window)
            val toShift: List<YearMonth> = (0..windowMonths).map { maxInTemplate.minusMonths(it) }

            // 4) Ren streng-replace i synkende rekkefølge for å unngå kjedereplassering
            var out = json
            toShift.sortedDescending().forEach { ym ->
                val from = ym.format(ymFormatter)
                val to = ym.plusMonths(delta).format(ymFormatter)
                out = out.replace(from, to)
            }
            return out
        }

        private fun collectYearMonths(
            node: JsonNode,
            out: MutableList<YearMonth>,
        ) {
            when {
                node.isTextual -> {
                    val text = node.textValue()
                    try {
                        out += YearMonth.parse(text, ymFormatter)
                    } catch (_: DateTimeParseException) {
                        // ikke en yyyy-MM tekstverdi; ignorer
                    }
                }
                node.isArray -> node.forEach { collectYearMonths(it, out) }
                node.isObject -> node.fields().forEachRemaining { (_, v) -> collectYearMonths(v, out) }
            }
        }
    }
}
