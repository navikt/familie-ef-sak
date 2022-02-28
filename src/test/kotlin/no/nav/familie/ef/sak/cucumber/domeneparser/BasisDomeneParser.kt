package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.vedtak.EndringType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val norskDatoFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val isoDatoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun parseDato(domenebegrep: Domenenøkkel, rad: Map<String, String>): LocalDate {
    return parseDato(domenebegrep.nøkkel(), rad)
}

fun parseValgfriDato(domenebegrep: Domenenøkkel, rad: Map<String, String?>): LocalDate? {
    return parseValgfriDato(domenebegrep.nøkkel(), rad)
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

fun parseBoolean(domenebegrep: Domenenøkkel, rad: Map<String, String>): Boolean {
    val verdi = verdi(domenebegrep.nøkkel(), rad)

    return when (verdi) {
        "Ja" -> true
        else -> false
    }
}

fun parseValgfriBoolean(domenebegrep: String, rad: Map<String, String?>): Boolean? {

    if (rad.get(domenebegrep) == null || rad.get(domenebegrep) == "") {
        return null
    }

    return when (rad.get(domenebegrep)) {
        "Ja" -> true
        "Nei" -> false
        else -> null
    }
}

fun parseDato(domenebegrep: String, rad: Map<String, String>): LocalDate {
    val dato = rad.get(domenebegrep)!!

    return if (dato.contains(".")) {
        LocalDate.parse(dato, norskDatoFormatter)
    } else {
        LocalDate.parse(dato, isoDatoFormatter)
    }
}

fun parseValgfriDato(domenebegrep: String, rad: Map<String, String?>): LocalDate? {
    if (rad.get(domenebegrep) == null || rad.get(domenebegrep) == "") {
        return null
    }
    val dato = rad.get(domenebegrep)!!

    return if (dato.contains(".")) {
        LocalDate.parse(dato, norskDatoFormatter)
    } else {
        LocalDate.parse(dato, isoDatoFormatter)
    }
}

fun verdi(nøkkel: String, rad: Map<String, String>): String {
    val verdi = rad.get(nøkkel)

    if (verdi == null || verdi == "") {
        throw java.lang.RuntimeException("Fant ingen verdi for $nøkkel")
    }

    return verdi
}

fun valgfriVerdi(nøkkel: String, rad: Map<String, String>): String? {
    val verdi = rad.get(nøkkel)

    return verdi
}

fun parseInt(domenebegrep: Domenenøkkel, rad: Map<String, String>): Int {
    val verdi = verdi(domenebegrep.nøkkel(), rad)

    return Integer.parseInt(verdi)
}

fun parseDouble(domenebegrep: Domenenøkkel, rad: Map<String, String>): Double {
    val verdi = verdi(domenebegrep.nøkkel(), rad)
    return verdi.toDouble()
}

fun parseValgfriDouble(domenebegrep: Domenenøkkel, rad: Map<String, String>): Double? {
    return valgfriVerdi(domenebegrep.nøkkel(), rad)?.toDouble() ?: return null
}

fun parseValgfriInt(domenebegrep: Domenenøkkel, rad: Map<String, String>): Int? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel(), rad)
    if (verdi == null) {
        return null
    }

    return parseInt(domenebegrep, rad)
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

fun <T> mapDataTable(dataTable: DataTable, radMapper: RadMapper<T>): List<T> {
    return dataTable.asMaps().map { radMapper.mapRad(it) }
}

interface RadMapper<T> {
    fun mapRad(rad: Map<String, String>): T
}