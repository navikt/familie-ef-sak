package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseAktivitetType
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseEndringType
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseInt
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseResultatType
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseString
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseValgfriDato
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseValgfriInt
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser.Companion.parseValgfriString
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.vedtak.HistorikkEndring
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.AbstractMap
import java.util.UUID

object VedtakDomeneParser {

    val behandlingIdTilUUID = mapOf(1 to UUID.randomUUID(), 2 to UUID.randomUUID(), 3 to UUID.randomUUID())
    val tilkjentYtelseIdNummerTilUUID = mapOf(1 to UUID.randomUUID(), 2 to UUID.randomUUID(), 3 to UUID.randomUUID())

    fun mapVedtak(dataTable: DataTable): List<Vedtak> {
        return dataTable.asMaps().map {
            VedtakMapper().mapRad(it)
        }
    }

    fun mapBehandlingForHistorikkEndring(dataTable: DataTable): List<Pair<UUID, HistorikkEndring?>> {
        return dataTable.asMaps().map {
            BehandlingForHistorikkEndringMapper().mapRad(it)
        }
    }


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

            val datoFra = parseValgfriDato(VedtakDomenebegrep.FRA_OG_MED_DATO, rad) ?: LocalDate.now()
            val datoTil = parseValgfriDato(VedtakDomenebegrep.TIL_OG_MED_DATO, rad) ?: LocalDate.now()
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
                    ))
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
                    stønadFom = parseValgfriDato(VedtakDomenebegrep.FRA_OG_MED_DATO, rad) ?: LocalDate.now(),
                    stønadTom = parseValgfriDato(VedtakDomenebegrep.TIL_OG_MED_DATO, rad) ?: LocalDate.now().plusYears(1),
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

        fun mapRad(rad: Map<String, String>): Pair<UUID, HistorikkEndring?> {
            if (parseEndringType(rad) == null && parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, rad) == null)
                return behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!! to null

            return behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, rad)]!! to
                    HistorikkEndring(
                            type = parseEndringType(rad)!!,
                            behandlingId = behandlingIdTilUUID[parseInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, rad)]!!,
                            vedtakstidspunkt = LocalDateTime.now()
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
    BEHANDLING_ID("BehandlingId"),
    ENDRET_I_BEHANDLING_ID("Endret i behandlingId"),
    ENDRING_TYPE("Endringstype"),
    HISTORIKKENDRING("Historikkendring"),
    ;

    override fun nøkkel(): String {
        return nøkkel
    }
}
