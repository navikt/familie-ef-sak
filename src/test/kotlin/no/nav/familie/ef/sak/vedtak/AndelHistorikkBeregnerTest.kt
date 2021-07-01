package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi.AndelHistorikkHeader.*
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.util.LinkedList
import java.util.UUID

//G-omregning splitter opp en periode i 2, der nr 2 skal få nytt tildato,

enum class HistorikkType {
    VANLIG,
    FJERNET,
    ENDRET,
    ENDRING_I_INNTEKT // mindre endring i inntekt som ikke endrer beløp
}

data class AndelHistorikk(val behandlingId: UUID,
                          val vedtaksdato: LocalDate,
                          val andel: AndelTilkjentYtelse,
                          val type: HistorikkType,
                          val endretI: UUID?)

object AndelHistorikkBeregner {

    private class AndelHistorikkHolder(val behandlingId: UUID,
                                       val vedtaksdato: LocalDate,
                                       var andel: AndelTilkjentYtelse,
                                       var type: HistorikkType,
                                       var endretI: UUID?,
                                       var kontrollert: UUID)

    private fun AndelTilkjentYtelse.endring(other: AndelTilkjentYtelse): HistorikkType? {
        return when {
            this.stønadTom != other.stønadTom || this.beløp != other.beløp -> HistorikkType.ENDRET
            this.inntekt != other.inntekt -> HistorikkType.ENDRING_I_INNTEKT
            else -> null
        }
    }

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>): List<AndelHistorikk> {
        val result = LinkedList(listOf<AndelHistorikkHolder>())

        tilkjentYtelser.forEach { tilkjentYtelse ->
            tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
                val tidligereAndel = finnTidligereAndel(result, andel)
                if (tidligereAndel == null) {
                    val index = result.indexOfFirst { it.andel.stønadFom.isAfter(andel.stønadTom) }
                    val nyHolder = nyHolder(tilkjentYtelse, andel)
                    if (index == -1) {
                        result.add(nyHolder)
                    } else {
                        result.add(index, nyHolder)
                    }
                } else {
                    val endringType = tidligereAndel.andel.endring(andel)
                    if (endringType != null) {
                        tidligereAndel.andel = andel //.copy(kildeBehandlingId = tidligereAndel.andel.kildeBehandlingId)
                        tidligereAndel.type = endringType
                        tidligereAndel.endretI = tilkjentYtelse.behandlingId
                    }
                    tidligereAndel.kontrollert = tilkjentYtelse.id
                }
            }

            result.filter { it.type != HistorikkType.FJERNET && it.kontrollert != tilkjentYtelse.id }.forEach {
                it.type = HistorikkType.FJERNET
                it.endretI = tilkjentYtelse.behandlingId
            }
        }

        return result.map {
            AndelHistorikk(it.behandlingId, it.vedtaksdato, it.andel, it.type, it.endretI)
        }
    }

    private fun nyHolder(tilkjentYtelse: TilkjentYtelse,
                         andel: AndelTilkjentYtelse) =
            AndelHistorikkHolder(tilkjentYtelse.behandlingId,
                                 tilkjentYtelse.vedtaksdato!!,
                                 andel,
                                 HistorikkType.VANLIG,
                                 null,
                                 tilkjentYtelse.id)

    private fun finnTidligereAndel(result: MutableList<AndelHistorikkHolder>,
                                   andel: AndelTilkjentYtelse) =
            result.find { it.type != HistorikkType.FJERNET && it.andel.stønadFom == andel.stønadFom }
}

class AndelHistorikkBeregnerTest {

    @Test
    internal fun `inntek_endrer_seg`() {
        run("/økonomi/inntekt_endrer_seg.csv")
    }

    @Test
    internal fun `periode2_slettes`() {
        run("/økonomi/periode2_slettes.csv")
    }

    @Test
    internal fun `periode2_slettes_og_får_en_ny_periode`() {
        run("/økonomi/periode2_slettes_og_får_en_ny_periode.csv")
    }

    @Test
    internal fun `periode_blir_lagt_til_på_nytt`() {
        run("/økonomi/periode_blir_lagt_til_på_nytt.csv")
    }

    @Test
    internal fun `periode_splittes`() {
        run("/økonomi/periode_splittes.csv")
    }

    private fun run(filnavn: String) {
        AndelHistorikkRunner.run(javaClass.getResource(filnavn))
    }
}

object AndelHistorikkRunner {

    fun run(url: URL) {
        val grupper = AndelHistorikkParser.parseGroup(url)

        val output = AndelHistorikkBeregner.lagHistorikk(grupper.input)

        assertThat(toString(output)).isEqualTo(toString(grupper.expectedOutput))
    }

    private val headerString = values().joinToString(", ") { mapValue(it, it.key) }

    private fun mapValue(key: AndelHistorikkHeader, value: Any?): String {
        return String.format("%-${key.minHeaderValue}s", value)
    }

    private fun toString(andeler: List<AndelHistorikk>): String {
        return "\n$headerString\n" +
               andeler.joinToString("\n") { andel ->
                   values().joinToString(", ") { mapValue(it, it.value.invoke(andel)) }
               } + "\n"
    }
}

private data class AndelHistorikkData(val erOutput: Boolean,
                                      val behandlingId: UUID,
                                      val andel: AndelTilkjentYtelse,
                                      val type: HistorikkType,
                                      val endretI: UUID?)

data class ParsetAndelHistorikkData(val input: List<TilkjentYtelse>,
                                    val expectedOutput: List<AndelHistorikk>)

private val oppdragIdn = mutableMapOf<Int, UUID>()
private fun generateBehandlingId(behandlingId: String): UUID = oppdragIdn.getOrPut(behandlingId.toInt()) { UUID.randomUUID() }
private fun hentBehandlingId(behandlingId: UUID) = oppdragIdn.entries.first { it.value == behandlingId }.key

private enum class AndelHistorikkHeader(val key: String,
                                        val value: (AndelHistorikk) -> Any?,
                                        val minHeaderValue: Int = key.length) {

    FOM("fom", { it.andel.stønadFom }, 11),
    TOM("tom", { it.andel.stønadTom }, 11),
    BELØP("beløp", { it.andel.beløp }, 8),
    INNTEKT("inntekt", { it.andel.inntekt }, 10),
    INNTEKTSREDUKSJON("inntektsreduksjon", { it.andel.inntektsreduksjon }),
    SAMORDNINGSFRADRAG("samordningsfradrag", { it.andel.samordningsfradrag }),
    BEHANDLING("behandling_id", { hentBehandlingId(it.behandlingId) }),
    TYPE_ENDRING("type_endring", { it.type }),
    ENDRET_I("endret_i", { it.endretI?.let(::hentBehandlingId) })
}

object AndelHistorikkParser {

    private const val PERSON_IDENT = "1"
    private const val OUTPUT = "OUTPUT"


    private fun parse(url: URL): List<AndelHistorikkData> {
        val fileContent = url.openStream()!!
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(fileContent)
                .filterNot { it.getValue(BEHANDLING.key).startsWith("!") }

        var erOutput = false

        return rows
                .map { row -> row.entries.map { it.key.trim() to it.value.trim() }.toMap() }
                .mapIndexedNotNull { index, row ->
                    try {
                        val behandlingIdStr = row.getValue(BEHANDLING.key)
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
                               AndelTilkjentYtelse(beløp = row.getInt(BELØP),
                                                   stønadFom = row.getValue(FOM)
                                                           .let { YearMonth.parse(it).atDay(1) },
                                                   stønadTom = row.getValue(TOM)
                                                           .let { YearMonth.parse(it).atEndOfMonth() },
                                                   personIdent = PERSON_IDENT,
                                                   inntekt = row.getInt(INNTEKT),
                                                   inntektsreduksjon = row.getInt(INNTEKTSREDUKSJON),
                                                   samordningsfradrag = row.getInt(SAMORDNINGSFRADRAG),
                                                   kildeBehandlingId = behandlingId),
                               row.getOptionalValue(TYPE_ENDRING)?.let { HistorikkType.valueOf(it) } ?: HistorikkType.VANLIG,
                               row.getOptionalValue(ENDRET_I)?.let { generateBehandlingId(it) })

    private fun Map<String, String>.getValue(header: AndelHistorikkHeader) = getValue(header.key)
    private fun Map<String, String>.getOptionalValue(header: AndelHistorikkHeader) = get(header.key)?.let { emptyAsNull(it) }
    private fun Map<String, String>.getInt(header: AndelHistorikkHeader) = getValue(header).toInt()

    private fun emptyAsNull(s: String?): String? =
            if (s == null || s.isEmpty()) null else s

    fun parseGroup(url: URL): ParsetAndelHistorikkData {
        val parse = parse(url)
        val groupBy = parse.groupBy { it.erOutput }
        return ParsetAndelHistorikkData(input = mapInput(groupBy[false]!!),
                                        expectedOutput = groupBy[true]!!.map {
                                            AndelHistorikk(it.behandlingId,
                                                           LocalDate.now(), // burde denne testes? EKs att man oppretter vedtaksdato per behandlingId
                                                           it.andel,
                                                           it.type,
                                                           it.endretI)
                                        })
    }

    private fun mapInput(input: List<AndelHistorikkData>): List<TilkjentYtelse> {
        return input
                .fold(mutableListOf<Pair<UUID, MutableList<AndelTilkjentYtelse>>>(), { acc, pair ->
                    val last = acc.lastOrNull()
                    if (last?.first != pair.behandlingId) {
                        acc.add(Pair(pair.behandlingId, mutableListOf(pair.andel)))
                    } else {
                        last.second.add(pair.andel)
                    }
                    acc
                })
                .map {
                    TilkjentYtelse(behandlingId = it.first,
                                   vedtaksdato = LocalDate.now(),
                                   andelerTilkjentYtelse = it.second,
                                   personident = PERSON_IDENT)
                }
    }
}