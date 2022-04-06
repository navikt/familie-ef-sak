package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriÅrMåned
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.YearMonth

class BeregningBarnetilsynEnPeriodeStepDefinitions {

    var inputData = mutableMapOf<String, PeriodeDataDto>()
    var resultat = mutableMapOf<String, BigDecimal>()

    @Gitt("følgende data")
    fun data(dataTable: DataTable) {
        dataTable.asMaps().map {
            val rad = it["Rad"] ?: error("Du må fylle inn radnummer i testen")
            val periodeDataDto = mapRadTilPeriodeDataDto(it)
            inputData.put(rad, periodeDataDto)
        }
    }

    private fun mapRadTilPeriodeDataDto(it: MutableMap<String, String>): PeriodeDataDto {
        val periodeutgift = it["Periodeutgift"]
        val kontrantstøttebeløp = it["Kontantstøttebeløp"]
        val tillegsstønadbeløp = it["Tillegsstønadbeløp"]
        val antallBarn = it["Antall barn"]
        val testkommentar: String? = it["Testkommentar"]
        val årMåned = parseValgfriÅrMåned("Periodedato", it)!!
        val periodeDataDto = PeriodeDataDto(periodeutgift = periodeutgift!!,
                                            kontrantstøttebeløp = kontrantstøttebeløp!!,
                                            tillegsønadbeløp = tillegsstønadbeløp!!,
                                            antallBarn = antallBarn!!,
                                            årMåned = årMåned,
                                            testkommentar = testkommentar)
        return periodeDataDto
    }

    @Når("vi beregner barnetilsyn beløp")
    fun `vi beregner barnetilsyn beløp`() {
        inputData.forEach { inputRad ->
            resultat.put(inputRad.key, beregnPeriodebeløp(inputRad))
        }
    }

    private fun beregnPeriodebeløp(it: Map.Entry<String, PeriodeDataDto>) =
            BeregningBarnetilsynUtil.beregnPeriodeBeløp(periodeutgift = it.value.periodeutgift.toBigDecimal(),
                                                             kontrantstøtteBeløp = it.value.kontrantstøttebeløp.toBigDecimal(),
                                                             tillegsønadBeløp = it.value.tillegsønadbeløp.toBigDecimal(),
                                                             antallBarn = it.value.antallBarn.toInt(),
                                                             årMåned = it.value.årMåned)

    @Så("forventer vi barnetilsyn periodebeløp")
    fun `forventer vi barnetilsyn periodebeløp`(dataTable: DataTable) {

        val feil = dataTable.asMaps().map { forventetData ->
            val rad = forventetData["Rad"]!!
            val periodeutgift = forventetData["Beløp"]!!.toBigDecimal()

            if (resultat[rad]!!.compareTo(periodeutgift) == 0) {
                null // alt ok
            } else {
                "Feilet på rad $rad: Her forventet vi $periodeutgift, men fikk ${resultat[rad]}, kommentar: ${inputData[rad]?.testkommentar} "
            }
        }.filterNotNull()

        assertThat(feil).hasSize(0).withFailMessage { "Vi fikk disse feilene: $feil" }
    }
}

data class PeriodeDataDto(val periodeutgift: String,
                          val kontrantstøttebeløp: String,
                          val tillegsønadbeløp: String,
                          val antallBarn: String,
                          val årMåned: YearMonth,
                          val testkommentar: String?)
