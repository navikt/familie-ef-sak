package no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.SaksbehandlingDomeneBegrep
import no.nav.familie.ef.sak.vedtak.EndringType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val norskDatoFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val norskÅrMånedFormatter = DateTimeFormatter.ofPattern("MM.yyyy")
val isoDatoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val isoÅrMånedFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

fun parseDato(domenebegrep: Domenenøkkel, rad: Map<String, String>): LocalDate {
    return parseDato(domenebegrep.nøkkel(), rad)
}

fun parseValgfriDato(domenebegrep: Domenenøkkel, rad: Map<String, String?>): LocalDate? {
    return parseValgfriDato(domenebegrep.nøkkel(), rad)
}

fun parseÅrMåned(domenebegrep: Domenenøkkel, rad: Map<String, String?>): YearMonth {
    return parseValgfriÅrMåned(domenebegrep.nøkkel(), rad)!!
}

fun parseValgfriÅrMåned(domenebegrep: Domenenøkkel, rad: Map<String, String?>): YearMonth? {
    return parseValgfriÅrMåned(domenebegrep.nøkkel(), rad)
}

fun parseString(domenebegrep: Domenenøkkel, rad: Map<String, String>): String {
    return verdi(domenebegrep.nøkkel(), rad)
}

fun parseValgfriString(domenebegrep: Domenenøkkel, rad: Map<String, String>): String? {
    return valgfriVerdi(domenebegrep.nøkkel(), rad)
}

fun parseBooleanMedBooleanVerdi(domenebegrep: Domenenøkkel, rad: Map<String, String>): Boolean {
    val verdi = verdi(domenebegrep.nøkkel(), rad)

    return when (verdi) {
        "true" -> true
        else -> false
    }
}

fun parseBooleanJaIsTrue(domenebegrep: Domenenøkkel, rad: Map<String, String>): Boolean {
    return when (valgfriVerdi(domenebegrep.nøkkel(), rad)) {
        "Ja" -> true
        else -> false
    }
}

fun parseBoolean(domenebegrep: Domenenøkkel, rad: Map<String, String>): Boolean {
    val verdi = verdi(domenebegrep.nøkkel(), rad)

    return when (verdi) {
        "Ja" -> true
        else -> false
    }
}

fun parseValgfriBoolean(domenebegrep: Domenenøkkel, rad: Map<String, String?>): Boolean? {

    val verdi = rad[domenebegrep.nøkkel()]
    if (verdi == null || verdi == "") {
        return null
    }

    return when (verdi) {
        "Ja" -> true
        "Nei" -> false
        else -> null
    }
}

fun parseDato(domenebegrep: String, rad: Map<String, String>): LocalDate {
    val dato = rad[domenebegrep]!!

    return if (dato.contains(".")) {
        LocalDate.parse(dato, norskDatoFormatter)
    } else {
        LocalDate.parse(dato, isoDatoFormatter)
    }
}

fun parseValgfriDato(domenebegrep: String, rad: Map<String, String?>): LocalDate? {
    val verdi = rad[domenebegrep]
    if (verdi == null || verdi == "") {
        return null
    }

    return if (verdi.contains(".")) {
        LocalDate.parse(verdi, norskDatoFormatter)
    } else {
        LocalDate.parse(verdi, isoDatoFormatter)
    }
}

fun parseValgfriÅrMåned(domenebegrep: String, rad: Map<String, String?>): YearMonth? {
    val verdi = rad[domenebegrep]
    if (verdi == null || verdi == "") {
        return null
    }

    return if (verdi.contains(".")) {
        YearMonth.parse(verdi, norskÅrMånedFormatter)
    } else {
        YearMonth.parse(verdi, isoÅrMånedFormatter)
    }
}

fun verdi(nøkkel: String, rad: Map<String, String>): String {
    val verdi = rad[nøkkel]

    if (verdi == null || verdi == "") {
        throw java.lang.RuntimeException("Fant ingen verdi for $nøkkel")
    }

    return verdi
}

fun valgfriVerdi(nøkkel: String, rad: Map<String, String>): String? {
    return rad[nøkkel]
}

fun parseInt(domenebegrep: Domenenøkkel, rad: Map<String, String>): Int {
    val verdi = verdi(domenebegrep.nøkkel(), rad)

    return Integer.parseInt(verdi)
}

fun parseBigDecimal(domenebegrep: Domenenøkkel, rad: Map<String, String>): BigDecimal {
    val verdi = verdi(domenebegrep.nøkkel(), rad)
    return verdi.toBigDecimal()
}

fun parseDouble(domenebegrep: Domenenøkkel, rad: Map<String, String>): Double {
    val verdi = verdi(domenebegrep.nøkkel(), rad)
    return verdi.toDouble()
}

fun parseValgfriDouble(domenebegrep: Domenenøkkel, rad: Map<String, String>): Double? {
    return valgfriVerdi(domenebegrep.nøkkel(), rad)?.toDouble() ?: return null
}

fun parseValgfriInt(domenebegrep: Domenenøkkel, rad: Map<String, String>): Int? {
    valgfriVerdi(domenebegrep.nøkkel(), rad) ?: return null

    return parseInt(domenebegrep, rad)
}

fun parseValgfriIntRange(domenebegrep: Domenenøkkel, rad: Map<String, String>): Pair<Int, Int>? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel(), rad) ?: return null

    return Pair(Integer.parseInt(verdi.split("-").first()),
                Integer.parseInt(verdi.split("-").last()))
}

fun parseResultatType(rad: Map<String, String>): ResultatType? {
    val verdi = valgfriVerdi(VedtakDomenebegrep.RESULTAT_TYPE.nøkkel, rad) ?: return null
    return ResultatType.valueOf(verdi)
}

fun parseEndringType(rad: Map<String, String>): EndringType? {
    val verdi = valgfriVerdi(VedtakDomenebegrep.ENDRING_TYPE.nøkkel, rad) ?: return null
    return EndringType.valueOf(verdi)
}

fun parseAktivitetType(rad: Map<String, String>): AktivitetType? {
    val verdi = valgfriVerdi(VedtakDomenebegrep.AKTIVITET_TYPE.nøkkel, rad) ?: return null
    return AktivitetType.valueOf(verdi)
}

fun parseArbeidAktivitet(rad: Map<String, String>): SvarId? {
    val verdi = valgfriVerdi(VedtakDomenebegrep.ARBEID_AKTIVITET.nøkkel, rad) ?: return null
    return SvarId.valueOf(verdi)
}


fun parseVedtaksperiodeType(rad: Map<String, String>): VedtaksperiodeType? {
    val verdi = valgfriVerdi(VedtakDomenebegrep.VEDTAKSPERIODE_TYPE.nøkkel, rad) ?: return null
    return VedtaksperiodeType.valueOf(verdi)
}

fun parseBehandlingstype(rad: Map<String, String>): BehandlingType? {
    val verdi = valgfriVerdi(SaksbehandlingDomeneBegrep.BEHANDLINGSTYPE.nøkkel, rad) ?: return null
    return BehandlingType.valueOf(verdi)
}

fun <T> mapDataTable(dataTable: DataTable, radMapper: RadMapper<T>): List<T> {
    return dataTable.asMaps().map { radMapper.mapRad(it) }
}

interface RadMapper<T> {

    fun mapRad(rad: Map<String, String>): T
}