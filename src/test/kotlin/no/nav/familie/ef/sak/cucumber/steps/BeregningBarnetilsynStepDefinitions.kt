package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.barnetilsyn.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.UtgiftsperiodeDto
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriÅrMåned
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

class BeregningBarnetilsynStepDefinitions {

    val beregningBarnetilsynService = BeregningBarnetilsynService()
    val kontantStøtteperioder: MutableList<PeriodeMedBeløpDto> = mutableListOf()
    val tilleggsstønadPerioder: MutableList<PeriodeMedBeløpDto> = mutableListOf()
    var beregnYtelseBarnetilsynResultat: MutableList<BeløpsperiodeBarnetilsynDto> = mutableListOf()
    val utgiftsperioder: MutableList<UtgiftsperiodeDto> = mutableListOf()


    @Gitt("utgiftsperioder")
    fun data(dataTable: DataTable) {
        dataTable.asMaps().map {
            val fraÅrMåned = parseValgfriÅrMåned("Fra måned", it)!!
            val tilÅrMåned = parseValgfriÅrMåned("Til og med måned", it)!!
            val beløp = it["Beløp"]!!.toBigDecimal()
            val barn = it["Antall barn"]!!.toInt()
            utgiftsperioder.add(UtgiftsperiodeDto(fraÅrMåned, tilÅrMåned, List(barn) { UUID.randomUUID() }, beløp))
        }
    }

    @Og("kontantstøtteperioder")
    fun kontantstøtteperioder(dataTable: DataTable) {
        dataTable.asMaps().map {
            val fraÅrMåned = parseValgfriÅrMåned("Fra måned", it)!!
            val tilÅrMåned = parseValgfriÅrMåned("Til og med måned", it)!!
            val beløp = it["Beløp"]!!.toBigDecimal()
            kontantStøtteperioder.add(PeriodeMedBeløpDto(fraÅrMåned, tilÅrMåned, beløp))
        }
    }

    @Og("tilleggsstønadsperioder")
    fun tilleggsstønadsperioder(dataTable: DataTable) {
        dataTable.asMaps().map {
            val fraÅrMåned = parseValgfriÅrMåned("Fra måned", it)!!
            val tilÅrMåned = parseValgfriÅrMåned("Til og med måned", it)!!
            val beløp = it["Beløp"]!!.toBigDecimal()
            tilleggsstønadPerioder.add(PeriodeMedBeløpDto(fraÅrMåned, tilÅrMåned, beløp))
        }
    }

    @Når("vi beregner perioder med barnetilsyn")
    fun `vi beregner perioder med barnetilsyn`() {
        beregnYtelseBarnetilsynResultat.addAll(beregningBarnetilsynService.beregnYtelseBarnetilsyn(utgiftsperioder = utgiftsperioder,
                                                                                                   kontantstøttePerioder = kontantStøtteperioder,
                                                                                                   tilleggsstønadsperioder = tilleggsstønadPerioder))
    }

    @Så("forventer vi følgende perioder")
    fun `forventer vi barnetilsyn periodebeløp`(dataTable: DataTable) {
        val forventet = dataTable.asMaps().map {
            val beløp = it["Beløp"]!!.toBigDecimal()
            val fraÅrMåned = parseValgfriÅrMåned("Fra måned", it)!!
            val tilÅrMåned = parseValgfriÅrMåned("Til og med måned", it)!!
            ForventetPeriode(beløp, fraÅrMåned, tilÅrMåned)
        }
        assertThat(beregnYtelseBarnetilsynResultat).size().isEqualTo(forventet.size)
        val sortedResultat = beregnYtelseBarnetilsynResultat.sortedBy { it.periode.fradato }
        val sortetForventet = forventet.sortedBy { it.fraÅrMåned }
        assertThat(sortedResultat.first().periode.fradato).isEqualTo(sortetForventet.first().fraÅrMåned.atDay(1))
        assertThat(sortedResultat.last().periode.fradato).isEqualTo(sortetForventet.last().fraÅrMåned.atDay(1))

        sortedResultat.forEachIndexed { idx, it ->
            assertThat(it.periode.fradato).isEqualTo(sortetForventet.get(idx).fraÅrMåned.atDay(1))
            assertThat(it.periode.tildato).isEqualTo(sortetForventet.get(idx).tilÅrMåned.atEndOfMonth())
            assertThat(it.beløp.compareTo(sortetForventet.get(idx).beløp)).isEqualTo(0).withFailMessage("Beløp var ${it.beløp}")
        }

    }

    data class ForventetPeriode(val beløp: BigDecimal, val fraÅrMåned: YearMonth, val tilÅrMåned: YearMonth)
}
