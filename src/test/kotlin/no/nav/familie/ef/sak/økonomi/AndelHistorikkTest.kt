package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.AndelHistorikkHeader.*
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.YearMonth
import java.util.UUID

object AndelHistorikk {

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>): List<AndelTilkjentYtelse> {
        return tilkjentYtelser.flatMap { it.andelerTilkjentYtelse }
    }
}

class AndelHistorikkTest {

    @Test
    internal fun `a b`() {
        run("1.csv")
    }

    private fun run(filnavn: String) {
        AndelHistorikkRunner.run(javaClass.getResource("/økonomi/$filnavn"))
    }
}

object AndelHistorikkRunner {

    fun run(url: URL) {
        val grupper = AndelHistorikkParser.parseGroup(url)

        val output = AndelHistorikk.lagHistorikk(grupper.input)

        assertThat(toString(output)).isEqualTo(toString(grupper.expectedOutput))
    }

    private val headerString = values().joinToString(", ") { mapValue(it, it.key) }

    private fun mapValue(key: AndelHistorikkHeader, value: Any): String {
        return String.format("%-${key.minHeaderValue}s", value)
    }

    private fun toString(andeler: List<AndelTilkjentYtelse>): String {
        return "\n$headerString\n" +
               andeler.joinToString("\n") { andel ->
                   values().joinToString(", ") { mapValue(it, it.value.invoke(andel)) }
               } + "\n"
    }
}

private data class AndelHistorikkData(val erOutput: Boolean, val behandlingId: UUID, val andelTilkjentYtelse: AndelTilkjentYtelse)
data class ParsetAndelHistorikkData(val input: List<TilkjentYtelse>,
                                    val expectedOutput: List<AndelTilkjentYtelse>)


private enum class AndelHistorikkHeader(val key: String,
                                        val value: (AndelTilkjentYtelse) -> Any,
                                        val minHeaderValue: Int = key.length) {

    KEY_FOM("fom", AndelTilkjentYtelse::stønadFom, 11),
    KEY_TOM("tom", AndelTilkjentYtelse::stønadFom, 11),
    KEY_BELØP("beløp", AndelTilkjentYtelse::beløp, 8),
    KEY_INNTEKT("inntekt", AndelTilkjentYtelse::inntekt, 10),
    KEY_INNTEKTSREDUKSJON("inntektsreduksjon", AndelTilkjentYtelse::inntektsreduksjon),
    KEY_SAMORDNINGSFRADRAG("samordningsfradrag", AndelTilkjentYtelse::samordningsfradrag),
    KEY_BEHANDLING("behandling_id", AndelTilkjentYtelse::kildeBehandlingId, UUID.randomUUID().toString().length)
}

object AndelHistorikkParser {

    private const val PERSON_IDENT = "1"
    private const val OUTPUT = "OUTPUT"


    private fun parse(url: URL): List<AndelHistorikkData> {
        val fileContent = url.openStream()!!
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(fileContent)
                .filterNot { it.getValue(KEY_BEHANDLING.key).startsWith("!") }

        var erOutput = false

        return rows
                .map { row -> row.entries.map { it.key.trim() to it.value.trim() }.toMap() }
                .mapIndexedNotNull { index, row ->
                    try {
                        val behandlingIdStr = row.getValue(KEY_BEHANDLING.key)
                        if (behandlingIdStr == OUTPUT) {
                            erOutput = true
                            return@mapIndexedNotNull null
                        }
                        val behandlingId = generateBehandlingId(behandlingIdStr)
                        mapRow(erOutput, behandlingId, row)
                    } catch (e: Exception) {
                        throw RuntimeException("Feilet håndtering av rowIndex=$index - $row", e)
                    }
                }
    }

    private fun mapRow(erOutput: Boolean,
                       behandlingId: UUID,
                       row: Map<String, String>) =
            AndelHistorikkData(erOutput,
                               behandlingId,
                               AndelTilkjentYtelse(beløp = row.getInt(KEY_BELØP),
                                                   stønadFom = row.getValue(KEY_FOM).let { YearMonth.parse(it).atDay(1) },
                                                   stønadTom = row.getValue(KEY_TOM).let { YearMonth.parse(it).atEndOfMonth() },
                                                   personIdent = PERSON_IDENT,
                                                   inntekt = row.getInt(KEY_INNTEKT),
                                                   inntektsreduksjon = row.getInt(KEY_INNTEKTSREDUKSJON),
                                                   samordningsfradrag = row.getInt(KEY_SAMORDNINGSFRADRAG),
                                                   kildeBehandlingId = behandlingId))

    private fun Map<String, String>.getValue(header: AndelHistorikkHeader) = getValue(header.key)
    private fun Map<String, String>.getInt(header: AndelHistorikkHeader) = getValue(header).toInt()

    private val oppdragIdn = mutableMapOf<Int, UUID>()

    private fun generateBehandlingId(behandlingId: String): UUID {
        return oppdragIdn.getOrPut(behandlingId.toInt()) { UUID.randomUUID() }
    }

    fun parseGroup(url: URL): ParsetAndelHistorikkData {
        val parse = parse(url)
        val groupBy = parse.groupBy { it.erOutput }
        return ParsetAndelHistorikkData(input = mapInput(groupBy[false]!!),
                                        expectedOutput = groupBy[true]!!.map { it.andelTilkjentYtelse })
    }

    private fun mapInput(input: List<AndelHistorikkData>): List<TilkjentYtelse> {
        return input
                .fold(mutableListOf<Pair<UUID, MutableList<AndelTilkjentYtelse>>>(), { acc, pair ->
                    val last = acc.lastOrNull()
                    if (last?.first != pair.behandlingId) {
                        acc.add(Pair(pair.behandlingId, mutableListOf(pair.andelTilkjentYtelse)))
                    } else {
                        last.second.add(pair.andelTilkjentYtelse)
                    }
                    acc
                })
                .map {
                    TilkjentYtelse(behandlingId = it.first,
                                   andelerTilkjentYtelse = it.second,
                                   personident = PERSON_IDENT)
                }
    }
}