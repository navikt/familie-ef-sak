package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.vedtak.HistorikkEndring
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.AbstractMap
import java.util.UUID

object VedtakDomeneParser {

    val behandlingIdTilUUID = (1..10).map { it to UUID.randomUUID() }.toMap()
    val tilkjentYtelseIdNummerTilUUID = (1..10).map { it to UUID.randomUUID() }.toMap()

    fun mapVedtak(dataTable: DataTable): List<Vedtak> {
        return dataTable.asMaps().groupBy {
            it.getValue(VedtakDomenebegrep.BEHANDLING_ID.nøkkel)
        }.map { (_, rader) ->

            val perioder = rader.map { rad ->
                val datoFra = parseValgfriÅrMåned(VedtakDomenebegrep.FRA_OG_MED_DATO, rad)?.atDay(1) ?: LocalDate.now()
                val datoTil = parseValgfriÅrMåned(VedtakDomenebegrep.TIL_OG_MED_DATO, rad)?.atEndOfMonth() ?: LocalDate.now()
                Vedtaksperiode(
                        datoFra = datoFra,
                        datoTil = datoTil,
                        aktivitet = parseAktivitetType(rad) ?: AktivitetType.BARN_UNDER_ETT_ÅR,
                        periodeType = parseVedtaksperiodeType(rad) ?: VedtaksperiodeType.HOVEDPERIODE
                )
            }
            val rad = rader.first()
            val resultatType = parseResultatType(rad) ?: ResultatType.INNVILGE
            Vedtak(
                    behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!,
                    resultatType = resultatType,
                    perioder = PeriodeWrapper(perioder),
                    inntekter = InntektWrapper(lagDefaultInntektsperiode(perioder)),
                    opphørFom = parseValgfriÅrMåned(VedtakDomenebegrep.OPPHØRSDATO, rad)?.atDay(1),
                    sanksjonsårsak = if (resultatType == ResultatType.SANKSJONERE) Sanksjonsårsak.NEKTET_TILBUDT_ARBEID else null,
                    internBegrunnelse = if (resultatType == ResultatType.SANKSJONERE) "Ok" else null

            )
        }
    }

    private fun lagDefaultInntektsperiode(perioder: List<Vedtaksperiode>) =
            perioder.firstOrNull()?.let {
                listOf(Inntektsperiode(it.datoFra,
                                       LocalDate.MAX,
                                       BigDecimal.ZERO,
                                       BigDecimal.ZERO))
            } ?: emptyList()

    fun mapInntekter(dataTable: DataTable): Map<UUID, InntektWrapper> {
        return dataTable.asMaps().groupBy {
            it.getValue(VedtakDomenebegrep.BEHANDLING_ID.nøkkel)
        }.values.associate { rader ->
            val inntektsperioder = rader.fold(mutableListOf<Inntektsperiode>()) { acc, rad ->
                val datoFra = parseValgfriÅrMåned(VedtakDomenebegrep.FRA_OG_MED_DATO, rad)?.atDay(1) ?: LocalDate.now()
                acc.removeLastOrNull()?.copy(sluttDato = datoFra.minusDays(1))?.let { acc.add(it) }
                acc.add(Inntektsperiode(datoFra,
                                        LocalDate.MAX,
                                        BigDecimal(parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad) ?: 0),
                                        BigDecimal(parseValgfriInt(VedtakDomenebegrep.SAMORDNINGSFRADRAG, rad) ?: 0)))
                acc
            }
            behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rader.first())]!! to InntektWrapper(inntektsperioder)
        }
    }

    fun mapBehandlingForHistorikkEndring(dataTable: DataTable): List<ForventetHistorikk> {
        return dataTable.asMaps().map {
            BehandlingForHistorikkEndringMapper().mapRad(it)
        }
    }

    class ForventetHistorikk(
            val behandlingId: UUID,
            val historikkEndring: HistorikkEndring?,
            val stønadFra: LocalDate,
            val stønadTil: LocalDate,
            val inntekt: Int?,
            val beløp: Int?,
            val aktivitetType: AktivitetType
    )

    fun mapAndelTilkjentYtelse(dataTable: DataTable): List<AndelTilkjentYtelse?> {
        return dataTable.asMaps().map {
            AndelTilkjentYtelseMapper().mapRad(it)
        }
    }

    fun lagDefaultTilkjentYtelseUtenAndel(dataTable: DataTable): MutableList<TilkjentYtelse> {
        val tilkjentYtelser = mutableListOf<TilkjentYtelse>()
        dataTable.asMaps().map {
            val behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, it)]!!
            tilkjentYtelser.add(lagTilkjentYtelse(listOf(), UUID.randomUUID(), behandlingId))
        }
        return tilkjentYtelser
    }

    fun lagDefaultTilkjentYtelseFraAndel(dataTable: DataTable): MutableList<TilkjentYtelse> {
        val behandlingIdTilAndelTilkjentYtelseList = mutableListOf<Pair<UUID, AndelTilkjentYtelse>>()
        dataTable.asMaps().map {
            val andel = AndelTilkjentYtelseMapper().mapRad(it)
            val behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, it)]!!
            val behandlingIdTilAndelTilkjentYtelse = AbstractMap.SimpleEntry(behandlingId, andel).toPair()
            behandlingIdTilAndelTilkjentYtelseList.add(behandlingIdTilAndelTilkjentYtelse)
        }

        val tilkjentYtelser = mutableListOf<TilkjentYtelse>()
        behandlingIdTilAndelTilkjentYtelseList.groupBy { it.first }.forEach {
            val andelerTilkjentYtelse = it.value.map { p -> p.second }
            tilkjentYtelser.add(lagTilkjentYtelse(andelerTilkjentYtelse, UUID.randomUUID(), it.key))
        }
        return tilkjentYtelser

    }

    class VedtakMapper {

        fun mapRad(rad: Map<String, String>): Vedtak {

            val datoFra = parseValgfriÅrMåned(VedtakDomenebegrep.FRA_OG_MED_DATO, rad)?.atDay(1) ?: LocalDate.now()
            val datoTil = parseValgfriÅrMåned(VedtakDomenebegrep.TIL_OG_MED_DATO, rad)?.atEndOfMonth() ?: LocalDate.now()
            return Vedtak(
                    behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!,
                    resultatType = parseResultatType(rad) ?: ResultatType.INNVILGE,
                    perioder = PeriodeWrapper(listOf(
                            Vedtaksperiode(
                                    datoFra = datoFra,
                                    datoTil = datoTil,
                                    aktivitet = parseAktivitetType(rad) ?: AktivitetType.BARN_UNDER_ETT_ÅR,
                                    periodeType = VedtaksperiodeType.HOVEDPERIODE
                            )
                    )),
                    opphørFom = parseValgfriÅrMåned(VedtakDomenebegrep.OPPHØRSDATO, rad)?.atDay(1)
            )
        }
    }

    class TilkjentYtelseMapper {

        fun mapRad(rad: Map<String, String>): TilkjentYtelse {
            return TilkjentYtelse(
                    id = tilkjentYtelseIdNummerTilUUID[parseInt(VedtakDomenebegrep.TILKJENT_YTELSE_ID, rad)]!!,
                    behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!,
                    personident = parseString(VedtakDomenebegrep.PERSONIDENT, rad),
                    vedtakstidspunkt = LocalDateTime.now(),
                    type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
                    andelerTilkjentYtelse = listOf()
            )
        }
    }

    class AndelTilkjentYtelseMapper {

        fun mapRad(rad: Map<String, String>): AndelTilkjentYtelse {
            return AndelTilkjentYtelse(
                    beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad) ?: 0,
                    stønadFom = parseValgfriÅrMåned(VedtakDomenebegrep.FRA_OG_MED_DATO, rad)?.atDay(1)
                                ?: LocalDate.now(),
                    stønadTom = parseValgfriÅrMåned(VedtakDomenebegrep.TIL_OG_MED_DATO, rad)?.atEndOfMonth()
                                ?: LocalDate.now().plusYears(1),
                    personIdent = parseValgfriString(VedtakDomenebegrep.PERSONIDENT, rad) ?: "1",
                    inntekt = parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad) ?: 0,
                    inntektsreduksjon = parseValgfriInt(VedtakDomenebegrep.INNTEKTSREDUKSJON, rad) ?: 0,
                    samordningsfradrag = parseValgfriInt(VedtakDomenebegrep.SAMORDNINGSFRADRAG, rad) ?: 0,
                    kildeBehandlingId = behandlingIdTilUUID[parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, rad)]
                                        ?: behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!
            )
        }
    }

    class BehandlingForHistorikkEndringMapper {

        fun mapRad(rad: Map<String, String>): ForventetHistorikk {
            return ForventetHistorikk(
                    behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!!,
                    historikkEndring = parseEndringType(rad)?.let { endringType ->
                        HistorikkEndring(
                                type = endringType,
                                behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, rad)]!!,
                                vedtakstidspunkt = LocalDateTime.now()
                        )
                    },
                    stønadFra = parseValgfriÅrMåned(VedtakDomenebegrep.FRA_OG_MED_DATO, rad)?.atDay(1) ?: YearMonth.now()
                            .atDay(1),
                    stønadTil = parseValgfriÅrMåned(VedtakDomenebegrep.TIL_OG_MED_DATO, rad)?.atEndOfMonth() ?: YearMonth.now()
                            .atEndOfMonth(),
                    inntekt = parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad),
                    beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad),
                    aktivitetType = parseAktivitetType(rad) ?: AktivitetType.BARN_UNDER_ETT_ÅR
            )
        }
    }

}

enum class VedtakDomenebegrep(val nøkkel: String) : Domenenøkkel {
    RESULTAT_TYPE("Vedtaksresultat"),
    TILKJENT_YTELSE_ID("Tilkjent ytelse Id"),
    PERSONIDENT("Personnummer"),
    INNTEKT("Inntekt"),
    INNTEKTSREDUKSJON("Inntektsreduksjon"),
    SAMORDNINGSFRADRAG("Samordningsfradrag"),
    BELØP("Beløp"),
    FRA_OG_MED_DATO("Fra og med dato"),
    TIL_OG_MED_DATO("Til og med dato"),
    AKTIVITET_TYPE("Aktivitet"),
    VEDTAKSPERIODE_TYPE("Vedtaksperiode"),
    BEHANDLING_ID("BehandlingId"),
    ENDRET_I_BEHANDLING_ID("Endret i behandlingId"),
    ENDRING_TYPE("Endringstype"),
    OPPHØRSDATO("Opphørsdato")
    ;

    override fun nøkkel(): String {
        return nøkkel
    }
}
