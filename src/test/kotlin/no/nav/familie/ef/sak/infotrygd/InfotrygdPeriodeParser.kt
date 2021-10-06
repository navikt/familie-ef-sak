package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.ef.sak.infotrygd.tilInternPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object InfotrygdPeriodeParser {

    data class InfotrygdTestData(val input: List<InfotrygdPeriode>, val output: List<InternPeriode>)

    private const val KEY_TYPE = "type"
    private const val KEY_STØNAD_ID = "stonad_id"
    private const val KEY_VEDTAK_ID = "vedtak_id"

    //private const val KEY_STØNAD_BELØP = "stonad_belop"
    private const val KEY_INNT_FRADRAG = "innt_fradrag"
    private const val KEY_SUM_FRADRAG = "sam_fradrag"
    private const val KEY_NETTO_BELØP = "netto_belop"
    private const val KEY_STØNAD_FOM = "dato_innv_fom"
    private const val KEY_STØNAD_TOM = "dato_innv_tom"
    private const val KEY_DATO_OPPHØR = "dato_opphor"

    private val DATO_FORMATTERER = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun parse(url: URL): InfotrygdTestData {
        val fileContent = url.openStream()!!
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(fileContent)

        val inputOutput = rows
                .map { row -> row.entries.associate { it.key.trim() to it.value } }
                .map { row ->
                    getValue(row, KEY_TYPE)!! to parseInfotrygdPeriode(row)
                }.groupBy({ it.first }, { it.second })
        return InfotrygdTestData(inputOutput["INPUT"]!!, inputOutput["OUTPUT"]!!.map{it.tilInternPeriode()})
    }

    private fun parseInfotrygdPeriode(row: Map<String, String>) =
        InfotrygdPeriode(stønadId = getValue(row, KEY_STØNAD_ID)!!.toLong(),
                          vedtakId = getValue(row, KEY_VEDTAK_ID)!!.toLong(),
                          inntektsreduksjon = getValue(row, KEY_INNT_FRADRAG)!!.toInt(),
                          samordningsfradrag = getValue(row, KEY_SUM_FRADRAG)!!.toInt(),
                          beløp = getValue(row, KEY_NETTO_BELØP)!!.toInt(),
                          stønadFom = LocalDate.parse(getValue(row, KEY_STØNAD_FOM)!!,
                                                         DATO_FORMATTERER),
                          stønadTom = LocalDate.parse(getValue(row, KEY_STØNAD_TOM)!!,
                                                         DATO_FORMATTERER),
                          opphørsdato = getValue(row, KEY_DATO_OPPHØR)
                                     ?.let { emptyAsNull(it) }
                                     ?.let { LocalDate.parse(it, DATO_FORMATTERER) },
                         personIdent = "",
                         brukerId = "",
                         kode = InfotrygdEndringKode.ENDRING_BEREGNINGSGRUNNLAG,
                         startDato = LocalDate.now(),
                         stønadBeløp = 0 // kanskje fjerne ?
            )

    private fun getValue(row: Map<String, String>, key: String) = row[key]?.trim()

    private fun emptyAsNull(s: String): String? = s.ifEmpty { null }

}