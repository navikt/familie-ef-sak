package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriode
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/*
KODE    	TEKST
CHAR(2) 	CHAR(30)
------- 	------------------------------
AN      	Annullert
AS      	Avgang småbarnstillegg
B       	Barnetillegg
BB      	Barn 18 år
E       	Endring i beregningsgrunnlag
F       	Førstegangsvedtak
G       	G-regulering
H       	Barn etterlatte.3 måneder
I       	Institusjonsopphold
IN      	Ny inntektsgr.
KB      	Barn 3 år
NB      	Nytt barn
NY      	Ny
O       	Opphørt
S       	Satsendring
TS      	Tilgang småbarnstillegg
U       	Uføregrad
UA      	Uaktuell
P       	Passivt
AV      	Avslag
OO      	Overf ny løsning

O 	44823
F 	123680
E 	285547
NY	19679
G 	242131
 */

object InfotrygdPeriodeParser {

    data class InfotrygdTestData(val input: List<InfotrygdPeriode>, val output: List<InfotrygdPeriode>)

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

        val inputOutput = rows.map { row ->
            getValue(row, KEY_TYPE)!! to parseInfotrygdPeriode(row)
        }.groupBy({ it.first }, { it.second })
        return InfotrygdTestData(inputOutput["INPUT"]!!, inputOutput["OUTPUT"]!!)
    }

    private fun parseInfotrygdPeriode(row: Map<String, String>) =
            InfotrygdPeriode(stønadId = getValue(row, KEY_STØNAD_ID)!!.toLong(),
                             vedtakId = getValue(row, KEY_VEDTAK_ID)!!.toLong(),
                             inntektsreduksjon = getValue(row, KEY_INNT_FRADRAG)!!.toInt(),
                             samordningsfradrag = getValue(row, KEY_INNT_FRADRAG)!!.toInt(),
                             beløp = getValue(row, KEY_NETTO_BELØP)!!.toInt(),
                             stønadFom = LocalDate.parse(getValue(row, KEY_STØNAD_FOM)!!,
                                                         DATO_FORMATTERER),
                             stønadTom = LocalDate.parse(getValue(row, KEY_STØNAD_TOM)!!,
                                                         DATO_FORMATTERER),
                             datoOpphor = getValue(row, KEY_DATO_OPPHØR)
                                     ?.let { emptyAsNull(it) }
                                     ?.let { LocalDate.parse(it, DATO_FORMATTERER) }
            )

    private fun getValue(row: Map<String, String>, key: String) = row[key]?.trim()

    private fun emptyAsNull(s: String): String? = s.ifEmpty { null }
}