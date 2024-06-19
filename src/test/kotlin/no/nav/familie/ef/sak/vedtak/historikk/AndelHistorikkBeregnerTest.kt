package no.nav.familie.ef.sak.vedtak.historikk

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.AKTIVITET
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.BEHANDLING
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.BELØP
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.ENDRET_I
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.FOM
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.INNTEKT
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.INNTEKTSREDUKSJON
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.PERIODE_TYPE
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.SAMORDNINGSFRADRAG
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.TEST_TYPE
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.TOM
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.TYPE_ENDRING
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkHeader.values
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AndelHistorikkBeregnerTest {
    @Test
    internal fun `periode forlenges`() {
        run("/økonomi/periode_forlenges.csv")
    }

    @Test
    internal fun `periode_erstatt_og_senere_fjernet_på_nytt`() {
        run("/økonomi/periode_erstatt_og_senere_fjernet_på_nytt.csv")
    }

    @Test
    internal fun `periode2_første_periode_endrer_seg`() {
        run("/økonomi/periode2_første_periode_endrer_seg.csv")
    }

    @Test
    internal fun `filtrering på tilOgMedBehandling på andre behandlingen`() {
        run("/økonomi/filtrer_tilOgMedBehandling_er_andre_behandlingen.csv", tilOgMedBehandlingId = 2)
    }

    @Test
    internal fun `revurerer behandling uten endringer`() {
        run("/økonomi/revurder_på_nytt_uten_endringer_som_først_splittets.csv")
    }

    @Test
    internal fun `revurderer behandling med nytt beløp, som sen revurderes uten endringer`() {
        run("/økonomi/revurder_på_nytt_uten_endringer.csv")
    }

    @Nested
    inner class Sanksjon {
        @Test
        internal fun `sanksjon midt i en periode`() {
            run("/økonomi/sanksjon_midt_i.csv")
        }

        @Test
        internal fun `sanksjon i slutten på en periode`() {
            run("/økonomi/sanksjon_slutten.csv")
        }

        @Test
        internal fun `sanksjon i starten på en periode`() {
            run("/økonomi/sanksjon_starten.csv")
        }

        @Test
        internal fun `revuderer sanksjon og setter tilbake til den første perioden på nytt`() {
            run("/økonomi/sanksjon_revurderes.csv")
        }

        @Test
        internal fun `revurderer en sanksjon der beløpet endres`() {
            run("/økonomi/sanksjon_revurdering_nytt_beløp.csv")
        }

        @Test
        internal fun `revurderer med sanksjon 2 ganger`() {
            run("/økonomi/sanksjon_flere.csv")
        }
    }

    private fun run(
        filnavn: String,
        tilOgMedBehandlingId: Int? = null,
    ) {
        AndelHistorikkRunner.run(javaClass.getResource(filnavn)!!, tilOgMedBehandlingId)
    }
}

object AndelHistorikkRunner {
    fun run(
        url: URL,
        tilOgMedBehandlingId: Int?,
    ) {
        val grupper = AndelHistorikkParser.parseGroup(url)

        validerInput(grupper)

        val now = LocalDateTime.now()
        val behandlinger =
            grupper.input.map { it.behandlingId }.distinct().mapIndexed { index, id ->
                behandling(id = id, opprettetTid = now.plusMinutes(index.toLong()), vedtakstidspunkt = LocalDateTime.now())
            }
        val behandlingId = tilOgMedBehandlingId?.let { generateBehandlingId(it) }

        val output =
            AndelHistorikkBeregner.lagHistorikk(
                StønadType.OVERGANGSSTØNAD,
                grupper.input,
                grupper.vedtaksliste,
                behandlinger,
                behandlingId,
                mapOf(),
                HistorikkKonfigurasjon(brukIkkeVedtatteSatser = true),
            )

        assertThat(toString(output)).isEqualTo(toString(grupper.expectedOutput))
    }

    private fun validerInput(grupper: ParsetAndelHistorikkData) {
        validerVedtaksperioderIkkeOverlapper(grupper)
        validerHarMaks1SanksjonPerVedtak(grupper)
    }

    private fun validerHarMaks1SanksjonPerVedtak(grupper: ParsetAndelHistorikkData) {
        grupper.vedtaksliste
            .mapNotNull { it.perioder?.perioder }
            .forEach { perioder -> perioder.count { it.periodeType == VedtaksperiodeType.SANKSJON } < 2 }
    }

    private fun validerVedtaksperioderIkkeOverlapper(grupper: ParsetAndelHistorikkData) {
        grupper.vedtaksliste.forEach { vedtak ->
            vedtak.perioder?.perioder?.fold(LocalDate.MIN) { acc, periode ->
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

    private fun mapValue(
        key: AndelHistorikkHeader,
        value: Any?,
    ): String = String.format("%-${key.minHeaderValue}s", value ?: "")

    private fun toString(andeler: List<AndelHistorikkDto>): String =
        "\n$headerString\n" +
            andeler.joinToString("\n") { andel ->
                values().joinToString(", ") { mapValue(it, it.value.invoke(andel)) }
            } + "\n"
}

enum class TestType {
    VEDTAK,
    ANDEL,
    OUTPUT,
}

private data class AndelHistorikkData(
    val testType: TestType,
    val behandlingId: UUID,
    val beløp: Int?,
    val stønadFom: LocalDate?,
    val stønadTom: LocalDate?,
    val inntekt: Int?,
    val inntektsreduksjon: Int?,
    val samordningsfradrag: Int?,
    val type: EndringType?,
    val aktivitet: AktivitetType?,
    val periodeType: VedtaksperiodeType?,
    val endretI: UUID?,
)

data class ParsetAndelHistorikkData(
    val vedtaksliste: List<Vedtak>,
    val input: List<TilkjentYtelse>,
    val expectedOutput: List<AndelHistorikkDto>,
)

private val oppdragIdn = mutableMapOf<Int, UUID>()

private fun generateBehandlingId(behandlingId: Int): UUID = oppdragIdn.getOrPut(behandlingId) { UUID.randomUUID() }

private fun hentBehandlingId(behandlingId: UUID) = oppdragIdn.entries.first { it.value == behandlingId }.key

/**
 * [ENDRET_I] kan brukes for å overstyre kildeBehandlingId
 */
private enum class AndelHistorikkHeader(
    val key: String,
    val value: (AndelHistorikkDto) -> Any?,
    val minHeaderValue: Int = key.length,
) {
    TEST_TYPE("type", { "" }),
    BEHANDLING("behandling_id", { hentBehandlingId(it.behandlingId) }),
    FOM("fom", { YearMonth.from(it.andel.stønadFra) }, 11),
    TOM("tom", { YearMonth.from(it.andel.stønadTil) }, 11),
    BELØP("beløp", { it.andel.beløp }, 8),
    INNTEKT("inntekt", { it.andel.inntekt }, 10),
    INNTEKTSREDUKSJON("inntektsreduksjon", { it.andel.inntektsreduksjon }),
    SAMORDNINGSFRADRAG("samordningsfradrag", { it.andel.samordningsfradrag }),
    AKTIVITET("aktivitet", { it.aktivitet }),
    PERIODE_TYPE("periode", { it.periodeType }),
    TYPE_ENDRING("type_endring", { it.endring?.type }),
    ENDRET_I("endret_i", { it.endring?.behandlingId?.let(::hentBehandlingId) }),
}

object AndelHistorikkParser {
    private const val PERSON_IDENT = "1"

    private fun parse(url: URL): List<AndelHistorikkData> {
        val fileContent = url.openStream()!!
        val rows: List<Map<String, String>> =
            csvReader()
                .readAllWithHeader(fileContent)
                .map { it.entries.associate { entry -> entry.key.trim() to entry.value.trim() } }
                .filterNot { it.getValue(TEST_TYPE.key).startsWith("!") }

        return rows
            .map { row -> row.entries.associate { it.key.trim() to it.value.trim() } }
            .mapIndexed { index, row ->
                try {
                    val behandlingIdInt = row.getValue(BEHANDLING.key).toInt()
                    val type = TestType.valueOf(row.getValue(TEST_TYPE.key))

                    val behandlingId = generateBehandlingId(behandlingIdInt)
                    mapRow(type, behandlingId, row)
                } catch (e: Exception) {
                    throw RuntimeException("Feilet håndtering av rowIndex=$index - $row", e)
                }
            }
    }

    private fun mapRow(
        type: TestType,
        behandlingId: UUID,
        row: Map<String, String>,
    ) =
        AndelHistorikkData(
            testType = type,
            behandlingId = behandlingId,
            beløp = row.getOptionalInt(BELØP),
            stønadFom = row.getOptionalValue(FOM)?.let { YearMonth.parse(it).atDay(1) },
            stønadTom = row.getOptionalValue(TOM)?.let { YearMonth.parse(it).atEndOfMonth() },
            inntekt = row.getOptionalInt(INNTEKT),
            inntektsreduksjon = row.getOptionalInt(INNTEKTSREDUKSJON),
            samordningsfradrag = row.getOptionalInt(SAMORDNINGSFRADRAG),
            aktivitet = row.getOptionalValue(AKTIVITET)?.let { AktivitetType.valueOf(it) },
            periodeType = row.getOptionalValue(PERIODE_TYPE)?.let { VedtaksperiodeType.valueOf(it) },
            type = row.getOptionalValue(TYPE_ENDRING)?.let { EndringType.valueOf(it) },
            endretI = row.getOptionalInt(ENDRET_I)?.let { generateBehandlingId(it) },
        )

    private fun mapAndel(andel: AndelHistorikkData): AndelTilkjentYtelse =
        AndelTilkjentYtelse(
            beløp = andel.beløp!!,
            stønadFom = andel.stønadFom!!,
            stønadTom = andel.stønadTom!!,
            personIdent = PERSON_IDENT,
            inntekt = andel.inntekt!!,
            inntektsreduksjon = andel.inntektsreduksjon!!,
            samordningsfradrag = andel.samordningsfradrag!!,
            kildeBehandlingId = andel.endretI ?: andel.behandlingId,
        )

    private fun Map<String, String>.getValue(header: AndelHistorikkHeader) = getValue(header.key)

    private fun Map<String, String>.getOptionalValue(header: AndelHistorikkHeader) = get(header.key)?.let { emptyAsNull(it) }

    private fun Map<String, String>.getOptionalInt(header: AndelHistorikkHeader) = getOptionalValue(header)?.toInt()

    private fun emptyAsNull(s: String?): String? =
        if (s == null || s.isEmpty()) null else s

    fun parseGroup(url: URL): ParsetAndelHistorikkData {
        val parse = parse(url)
        val groupBy = parse.groupBy { it.testType }
        return ParsetAndelHistorikkData(
            vedtaksliste = mapVedtaksPerioder(groupBy[TestType.VEDTAK]!!),
            input = mapInput(groupBy[TestType.ANDEL]!!),
            expectedOutput = groupBy[TestType.OUTPUT]?.map { lagAndel(it) } ?: emptyList(),
        )
    }

    /**
     * Mapper vedtak med kun startdato til å være av typen opphør
     */
    private fun mapVedtaksPerioder(list: List<AndelHistorikkData>): List<Vedtak> =
        list
            .map { it.behandlingId to it }
            .groupBy({ it.first }, { it.second })
            .map { (behandlingId, vedtaksperioder) ->
                val resultat: ResultatType
                var periodeWrapper: PeriodeWrapper? = null
                var opphørFom: YearMonth? = null
                if (vedtaksperioder.singleOrNull()?.takeIf { it.periodeType == VedtaksperiodeType.SANKSJON } != null) {
                    resultat = ResultatType.SANKSJONERE
                    periodeWrapper = mapVedtaksperioder(vedtaksperioder, Sanksjonsårsak.SAGT_OPP_STILLING)
                } else if (vedtaksperioder.all { it.stønadFom != null && it.stønadTom != null }) {
                    resultat = ResultatType.INNVILGE
                    periodeWrapper = mapVedtaksperioder(vedtaksperioder)
                } else {
                    feilHvis(vedtaksperioder.size > 1) {
                        "Kan kun være en vedtaksperiode som er av typen opphør"
                    }
                    resultat = ResultatType.OPPHØRT
                    opphørFom = YearMonth.from(vedtaksperioder.single().stønadFom) ?: error("Mangler stønadFom i opphør")
                }
                val inntekter =
                    periodeWrapper
                        ?.perioder
                        ?.firstOrNull()
                        ?.let {
                            listOf(
                                Inntektsperiode(
                                    periode = Månedsperiode(it.datoFra, it.datoTil),
                                    inntekt = BigDecimal.ZERO,
                                    samordningsfradrag = BigDecimal.ZERO,
                                ),
                            )
                        } ?: emptyList()
                Vedtak(
                    behandlingId = behandlingId,
                    resultatType = resultat,
                    periodeBegrunnelse = null,
                    inntektBegrunnelse = null,
                    avslåBegrunnelse = null,
                    perioder = periodeWrapper,
                    inntekter = InntektWrapper(inntekter),
                    saksbehandlerIdent = null,
                    opphørFom = opphørFom,
                    beslutterIdent = null,
                    internBegrunnelse = "",
                )
            }

    private fun mapVedtaksperioder(
        vedtaksperioder: List<AndelHistorikkData>,
        sanksjonsårsak: Sanksjonsårsak? = null,
    ) =
        PeriodeWrapper(
            vedtaksperioder.map {
                Vedtaksperiode(
                    datoFra = it.stønadFom!!,
                    datoTil = it.stønadTom!!,
                    aktivitet = it.aktivitet!!,
                    periodeType = it.periodeType!!,
                    sanksjonsårsak = sanksjonsårsak,
                )
            },
        )

    private fun lagAndel(it: AndelHistorikkData) =
        AndelHistorikkDto(
            behandlingId = it.behandlingId,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            vedtakstidspunkt = LocalDateTime.now(), // burde denne testes? EKs att man oppretter vedtaksdato per behandlingId
            saksbehandler = "",
            vedtaksperiode =
                VedtakshistorikkperiodeOvergangsstønad(
                    periode = Månedsperiode(it.stønadFom!!, it.stønadFom!!),
                    aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                    periodeType = VedtaksperiodeType.HOVEDPERIODE,
                    inntekt = Inntekt(YearMonth.from(it.stønadFom), BigDecimal.ZERO, BigDecimal.ZERO),
                ),
            andel = AndelMedGrunnlagDto(mapAndel(it), null),
            aktivitet = it.aktivitet!!,
            periodeType = it.periodeType!!,
            endring =
                it.type?.let { type ->
                    HistorikkEndring(
                        type,
                        it.endretI
                            ?: error("Trenger id til behandling hvis det finnes en endring"),
                        LocalDateTime.now(),
                    )
                },
            aktivitetArbeid = null,
            erSanksjon = false,
            sanksjonsårsak = null,
            erOpphør = false,
            periodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
            aktivitetBarnetilsyn = AktivitetstypeBarnetilsyn.I_ARBEID,
        )

    data class AndelTilkjentHolder(
        val behandlingId: UUID,
        val andeler: MutableList<AndelTilkjentYtelse?>,
    )

    /**
     * Mapper andel uten start og sluttdato til en [TilkjentYtelse] med tom liste av andeler
     */
    private fun mapInput(input: List<AndelHistorikkData>): List<TilkjentYtelse> =
        input
            .fold(mutableListOf<AndelTilkjentHolder>()) { acc, pair ->
                val last = acc.lastOrNull()
                val andel = if (pair.stønadFom == null || pair.stønadTom == null) null else mapAndel(pair)
                if (last?.behandlingId != pair.behandlingId) {
                    acc.add(AndelTilkjentHolder(pair.behandlingId, mutableListOf(andel)))
                } else {
                    last.andeler.add(andel)
                }
                acc
            }.map { holder ->
                @Suppress("UNCHECKED_CAST")
                val andelerTilkjentYtelse =
                    if (holder.andeler.contains(null)) {
                        feilHvis(holder.andeler.size > 1) {
                            "Andeler kan kun inneholde ett element som mangler stønadFom/stønadTom savnes"
                        }
                        emptyList()
                    } else {
                        holder.andeler as List<AndelTilkjentYtelse>
                    }

                TilkjentYtelse(
                    behandlingId = holder.behandlingId,
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    personident = PERSON_IDENT,
                    startdato = andelerTilkjentYtelse.minOfOrNull { it.stønadFom } ?: LocalDate.now(),
                )
            }
}
