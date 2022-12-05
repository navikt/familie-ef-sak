package no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.cucumber.domeneparser.parseBigDecimal
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseÅrMåned
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.ANTALL_BARN
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.KONTANTSTØTTEBELØP
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.PERIODEDATO
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.PERIODEUTGIFT
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.TILLEGSSTØNADBELØP
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.YearMonth

class BeregningBarnetilsynEnPeriodeStepDefinitions {

    lateinit var resultat: BigDecimal
    lateinit var inputData: PeriodeDataDto

    @Gitt("følgende data")
    fun data(dataTable: DataTable) {
        inputData = dataTable.asMaps().map {
            PeriodeDataDto(
                periodeutgift = parseBigDecimal(PERIODEUTGIFT, it),
                kontantstøtteBeløp = parseBigDecimal(KONTANTSTØTTEBELØP, it),
                tillegstønadbeløp = parseBigDecimal(TILLEGSSTØNADBELØP, it),
                antallBarn = parseInt(ANTALL_BARN, it),
                årMåned = parseÅrMåned(PERIODEDATO, it)
            )
        }.first()
    }

    @Når("vi beregner barnetilsyn beløp")
    fun `vi beregner barnetilsyn beløp`() {
        resultat = beregnPeriodebeløp(inputData).utbetaltBeløp
    }

    private fun beregnPeriodebeløp(periodeDataDto: PeriodeDataDto) =
        BeregningBarnetilsynUtil.beregnPeriodeBeløp(
            periodeutgift = periodeDataDto.periodeutgift,
            kontantstøtteBeløp = periodeDataDto.kontantstøtteBeløp,
            tilleggsstønadBeløp = periodeDataDto.tillegstønadbeløp,
            antallBarn = periodeDataDto.antallBarn,
            årMåned = periodeDataDto.årMåned,
            false
        )

    @Så("forventer vi barnetilsyn periodebeløp")
    fun `forventer vi barnetilsyn periodebeløp`(dataTable: DataTable) {
        val forventetBeløp = dataTable.asMaps().map { it["Beløp"]!!.toBigDecimal() }.first()
        assertThat(resultat).isEqualByComparingTo(forventetBeløp)
    }
}

data class PeriodeDataDto(
    val periodeutgift: BigDecimal,
    val kontantstøtteBeløp: BigDecimal,
    val tillegstønadbeløp: BigDecimal,
    val antallBarn: Int,
    val årMåned: YearMonth
)
