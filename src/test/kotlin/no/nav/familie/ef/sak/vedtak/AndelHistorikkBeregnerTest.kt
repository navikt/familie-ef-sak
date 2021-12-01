package no.nav.familie.ef.sak.vedtak

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.tilDto
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.AKTIVITET
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.BEHANDLING
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.BELØP
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.ENDRET_I
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.FOM
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.INNTEKT
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.INNTEKTSREDUKSJON
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.PERIODE_TYPE
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.SAMORDNINGSFRADRAG
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.TEST_TYPE
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.TOM
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.TYPE_ENDRING
import no.nav.familie.ef.sak.vedtak.AndelHistorikkHeader.values
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AndelHistorikkBeregnerTest {

    @Test
    internal fun `inntek_endrer_seg`() {
        run("/økonomi/inntekt_endrer_seg.csv")
    }

    @Test
    internal fun `når vi revurderer fra midt i en tidligere periode lagrer vi ikke ned hele vedtakshistorikken`() {
            run("/økonomi/hele_vedtaket_blir_ikke_med.csv")
    }

    @Test
    internal fun `aktivitet og vedtaksperiodetype endrer seg`() {
        run("/økonomi/aktivitet_periodetype_endrer_seg.csv")
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
        run("/økonomi/periode_splittes_g_omregning.csv")
    }

    @Test
    internal fun `periode_splittes 2`() {
        run("/økonomi/periode_splittes.csv")
    }

    private fun run(filnavn: String) {
        AndelHistorikkRunner.run(javaClass.getResource(filnavn))
    }
}

object AndelHistorikkRunner {

    fun run(url: URL) {
        val grupper = AndelHistorikkParser.parseGroup(url)

        validerInput(grupper)

        val behandlinger = grupper.input.map { it.behandlingId }.distinct().map { behandling(id = it) }

        val output = AndelHistorikkBeregner.lagHistorikk(grupper.input, grupper.vedtaksliste, behandlinger)

        assertThat(toString(output)).isEqualTo(toString(grupper.expectedOutput))
    }

    private fun validerInput(grupper: ParsetAndelHistorikkData) {
        validerVedtaksperioderIkkeOverlapper(grupper)
    }

    private fun validerVedtaksperioderIkkeOverlapper(grupper: ParsetAndelHistorikkData) {
        grupper.vedtaksliste.forEach { vedtak ->
            vedtak.perioder!!.perioder.fold(LocalDate.MIN) { acc, periode ->
                require(periode.datoFra > acc) {
                    "Fra-dato for ${hentBehandlingId(vedtak.behandlingId)} (${periode.datoFra}) må være etter $acc"
                }
                require(periode.datoFra < periode.datoTil) {
                    "Fra-dato for ${hentBehandlingId(vedtak.behandlingId)} (${periode.datoFra}) må være før $${periode.datoTil}"
                }
                periode.datoTil
            }
        }
    }

    private val headerString = values().joinToString(", ") { mapValue(it, it.key) }

    private fun mapValue(key: AndelHistorikkHeader, value: Any?): String {
        return String.format("%-${key.minHeaderValue}s", value)
    }

    private fun toString(andeler: List<AndelHistorikkDto>): String {
        return "\n$headerString\n" +
               andeler.joinToString("\n") { andel ->
                   values().joinToString(", ") { mapValue(it, it.value.invoke(andel)) }
               } + "\n"
    }
}

enum class TestType {
    VEDTAK,
    ANDEL,
    OUTPUT
}

private data class AndelHistorikkData(val testType: TestType,
                                      val behandlingId: UUID,
                                      val beløp: Int?,
                                      val stønadFom: LocalDate,
                                      val stønadTom: LocalDate,
                                      val inntekt: Int?,
                                      val inntektsreduksjon: Int?,
                                      val samordningsfradrag: Int?,
                                      val type: EndringType?,
                                      val aktivitet: AktivitetType?,
                                      val periodeType: VedtaksperiodeType?,
                                      val endretI: UUID?)

data class ParsetAndelHistorikkData(val vedtaksliste: List<Vedtak>,
                                    val input: List<TilkjentYtelse>,
                                    val expectedOutput: List<AndelHistorikkDto>)

private val oppdragIdn = mutableMapOf<Int, UUID>()
private fun generateBehandlingId(behandlingId: String): UUID = oppdragIdn.getOrPut(behandlingId.toInt()) { UUID.randomUUID() }
private fun hentBehandlingId(behandlingId: UUID) = oppdragIdn.entries.first { it.value == behandlingId }.key

private enum class AndelHistorikkHeader(val key: String,
                                        val value: (AndelHistorikkDto) -> Any?,
                                        val minHeaderValue: Int = key.length) {

    TEST_TYPE("type", {""}),
    FOM("fom", { it.andel.stønadFra }, 11),
    TOM("tom", { it.andel.stønadTil }, 11),
    BELØP("beløp", { it.andel.beløp }, 8),
    INNTEKT("inntekt", { it.andel.inntekt }, 10),
    INNTEKTSREDUKSJON("inntektsreduksjon", { it.andel.inntektsreduksjon }),
    SAMORDNINGSFRADRAG("samordningsfradrag", { it.andel.samordningsfradrag }),
    BEHANDLING("behandling_id", { hentBehandlingId(it.behandlingId) }),
    AKTIVITET("aktivitet", { it.aktivitet }),
    PERIODE_TYPE("periode", { it.periodeType }),
    TYPE_ENDRING("type_endring", { it.endring?.type }),
    ENDRET_I("endret_i", { it.endring?.behandlingId?.let(::hentBehandlingId) })
}

object AndelHistorikkParser {

    private const val PERSON_IDENT = "1"

    private fun parse(url: URL): List<AndelHistorikkData> {
        val fileContent = url.openStream()!!
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(fileContent)
                .map { it.entries.associate { entry -> entry.key.trim() to entry.value.trim() } }
                .filterNot { it.getValue(TEST_TYPE.key).startsWith("!") }

        return rows
                .map { row -> row.entries.associate { it.key.trim() to it.value.trim() } }
                .mapIndexed { index, row ->
                    try {
                        val behandlingIdStr = row.getValue(BEHANDLING.key)
                        val type = TestType.valueOf(row.getValue(TEST_TYPE.key))

                        val behandlingId = generateBehandlingId(behandlingIdStr)
                        mapRow(type, behandlingId, row)
                    } catch (e: Exception) {
                        throw RuntimeException("Feilet håndtering av rowIndex=$index - $row", e)
                    }
                }
    }

    private fun mapRow(type: TestType,
                       behandlingId: UUID,
                       row: Map<String, String>) =
            AndelHistorikkData(testType = type,
                               behandlingId = behandlingId,
                               beløp = row.getOptionalInt(BELØP),
                               stønadFom = row.getValue(FOM).let { YearMonth.parse(it).atDay(1) },
                               stønadTom = row.getValue(TOM).let { YearMonth.parse(it).atEndOfMonth() },
                               inntekt = row.getOptionalInt(INNTEKT),
                               inntektsreduksjon = row.getOptionalInt(INNTEKTSREDUKSJON),
                               samordningsfradrag = row.getOptionalInt(SAMORDNINGSFRADRAG),
                               aktivitet = row.getOptionalValue(AKTIVITET)?.let { AktivitetType.valueOf(it) },
                               periodeType = row.getOptionalValue(PERIODE_TYPE)?.let { VedtaksperiodeType.valueOf(it) },
                               type = row.getOptionalValue(TYPE_ENDRING)?.let { EndringType.valueOf(it) },
                               endretI = row.getOptionalValue(ENDRET_I)?.let { generateBehandlingId(it) })

    private fun mapAndel(andel: AndelHistorikkData): AndelTilkjentYtelse =
            AndelTilkjentYtelse(beløp = andel.beløp!!,
                                stønadFom = andel.stønadFom,
                                stønadTom = andel.stønadTom,
                                personIdent = PERSON_IDENT,
                                inntekt = andel.inntekt!!,
                                inntektsreduksjon = andel.inntektsreduksjon!!,
                                samordningsfradrag = andel.samordningsfradrag!!,
                                kildeBehandlingId = andel.endretI ?: andel.behandlingId)

    private fun Map<String, String>.getValue(header: AndelHistorikkHeader) = getValue(header.key)
    private fun Map<String, String>.getOptionalValue(header: AndelHistorikkHeader) = get(header.key)?.let { emptyAsNull(it) }
    private fun Map<String, String>.getOptionalInt(header: AndelHistorikkHeader) = getOptionalValue(header)?.toInt()

    private fun emptyAsNull(s: String?): String? =
            if (s == null || s.isEmpty()) null else s

    fun parseGroup(url: URL): ParsetAndelHistorikkData {
        val parse = parse(url)
        val groupBy = parse.groupBy { it.testType }
        return ParsetAndelHistorikkData(vedtaksliste = mapVedtaksPerioder(groupBy[TestType.VEDTAK]!!),
                                        input = mapInput(groupBy[TestType.ANDEL]!!),
                                        expectedOutput = groupBy[TestType.OUTPUT]!!.map { lagAndel(it) })
    }

    private fun mapVedtaksPerioder(list: List<AndelHistorikkData>): List<Vedtak> {
        return list.map { it.behandlingId to it }
                .groupBy({ it.first }, { it.second })
                .map { (behandlingId, vedtaksperioder) ->
                    Vedtak(behandlingId = behandlingId,
                           resultatType = ResultatType.INNVILGE,
                           periodeBegrunnelse = null,
                           inntektBegrunnelse = null,
                           avslåBegrunnelse = null,
                           perioder = PeriodeWrapper(vedtaksperioder.map {
                               Vedtaksperiode(datoFra = it.stønadFom,
                                              datoTil = it.stønadTom,
                                              aktivitet = it.aktivitet!!,
                                              periodeType = it.periodeType!!)
                           }),
                           inntekter = null,
                           saksbehandlerIdent = null,
                           opphørFom = null,
                           beslutterIdent = null)
                }
    }

    private fun lagAndel(it: AndelHistorikkData) =
            AndelHistorikkDto(behandlingId = it.behandlingId,
                              behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                              vedtakstidspunkt = LocalDateTime.now(), // burde denne testes? EKs att man oppretter vedtaksdato per behandlingId
                              saksbehandler = "",
                              andel = mapAndel(it).tilDto(),
                              aktivitet = it.aktivitet!!,
                              periodeType = it.periodeType!!,
                              endring = it.type?.let { type ->
                                  HistorikkEndring(type,
                                                   it.endretI
                                                   ?: error("Trenger id til behandling hvis det finnes en endring"),
                                                   LocalDateTime.now())
                              })

    private fun mapInput(input: List<AndelHistorikkData>): List<TilkjentYtelse> {
        return input
                .fold(mutableListOf<Pair<UUID, MutableList<AndelTilkjentYtelse>>>()) { acc, pair ->
                    val last = acc.lastOrNull()
                    val andel = mapAndel(pair)
                    if (last?.first != pair.behandlingId) {
                        acc.add(Pair(pair.behandlingId, mutableListOf(andel)))
                    } else {
                        last.second.add(andel)
                    }
                    acc
                }
                .map {
                    TilkjentYtelse(behandlingId = it.first,
                                   vedtakstidspunkt = LocalDateTime.now(),
                                   andelerTilkjentYtelse = it.second,
                                   personident = PERSON_IDENT)
                }
    }
}