package no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriÅrMåned
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.YearMonth

class BeregningBarnetilsynEnPeriodeStepDefinitions {

    lateinit var resultat: BigDecimal
    lateinit var inputData: PeriodeDataDto

    @Gitt("følgende data")
    fun data(dataTable: DataTable) {
        inputData = dataTable.asMaps().map { mapRadTilPeriodeDataDto(it) }.first()
    }

    private fun mapRadTilPeriodeDataDto(it: MutableMap<String, String>): PeriodeDataDto {
        val periodeutgift = it["Periodeutgift"]
        val kontantstøttebeløp = it["Kontantstøttebeløp"]
        val tillegsstønadbeløp = it["Tillegsstønadbeløp"]
        val antallBarn = it["Antall barn"]
        val årMåned = parseValgfriÅrMåned("Periodedato", it)!!
        val periodeDataDto = PeriodeDataDto(periodeutgift = periodeutgift!!,
                                            kontantstøtteBeløp = kontantstøttebeløp!!,
                                            tillegstønadbeløp = tillegsstønadbeløp!!,
                                            antallBarn = antallBarn!!,
                                            årMåned = årMåned)
        return periodeDataDto
    }

    @Når("vi beregner barnetilsyn beløp")
    fun `vi beregner barnetilsyn beløp`() {
        resultat = beregnPeriodebeløp(inputData)
    }

    private fun beregnPeriodebeløp(periodeDataDto: PeriodeDataDto) =
            BeregningBarnetilsynUtil.beregnPeriodeBeløp(periodeutgift = periodeDataDto.periodeutgift.toBigDecimal(),
                                                        kontantstøtteBeløp = periodeDataDto.kontantstøtteBeløp.toBigDecimal(),
                                                        tillegstønadBeløp = periodeDataDto.tillegstønadbeløp.toBigDecimal(),
                                                        antallBarn = periodeDataDto.antallBarn.toInt(),
                                                        årMåned = periodeDataDto.årMåned)

    @Så("forventer vi barnetilsyn periodebeløp")
    fun `forventer vi barnetilsyn periodebeløp`(dataTable: DataTable) {
        val forventetBeløp = dataTable.asMaps().map { it["Beløp"]!!.toBigDecimal() }.first()
        assertThat(forventetBeløp).isEqualByComparingTo(resultat)
    }
}

data class PeriodeDataDto(val periodeutgift: String,
                          val kontantstøtteBeløp: String,
                          val tillegstønadbeløp: String,
                          val antallBarn: String,
                          val årMåned: YearMonth)
