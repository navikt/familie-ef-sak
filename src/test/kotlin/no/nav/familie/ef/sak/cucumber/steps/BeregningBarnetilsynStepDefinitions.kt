package no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.cucumber.domeneparser.parseAktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.cucumber.domeneparser.parseBoolean
import no.nav.familie.ef.sak.cucumber.domeneparser.parseBooleanJaIsTrue
import no.nav.familie.ef.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parsePeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.cucumber.domeneparser.parseÅrMåned
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.ANTALL_BARN
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.BELØP
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.FRA_MND
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.HAR_KONTANTSTØTTE
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.HAR_TILLEGGSSTØNAD
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BeregningBarnetilsynDomenebegrep.TIL_OG_MED_MND
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegelUtil
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BeregningBarnetilsynStepDefinitions {
    private lateinit var gittFødselsdato: LocalDate
    private var datoForKjøring: LocalDate? = null
    private var resultatHarFullførtFjerdetrinn: Boolean? = null

    val featureToggleService = mockFeatureToggleService()
    val beregningBarnetilsynService = BeregningBarnetilsynService(featureToggleService)
    val kontantStøtteperioder: MutableList<PeriodeMedBeløpDto> = mutableListOf()
    val tilleggsstønadPerioder: MutableList<PeriodeMedBeløpDto> = mutableListOf()
    var beregnYtelseBarnetilsynResultat: MutableList<BeløpsperiodeBarnetilsynDto> = mutableListOf()
    val utgiftsperioder: MutableList<UtgiftsperiodeDto> = mutableListOf()
    val barnPåIndex = List(10) { UUID.randomUUID() }

    @Gitt("utgiftsperioder")
    fun data(dataTable: DataTable) {
        dataTable.asMaps().map {
            val periodetype = parsePeriodetypeBarnetilsyn(it) ?: PeriodetypeBarnetilsyn.ORDINÆR
            val aktivitetstype = parseAktivitetstypeBarnetilsyn(it)
            val fraÅrMåned = parseÅrMåned(FRA_MND, it)
            val tilÅrMåned = parseÅrMåned(TIL_OG_MED_MND, it)
            val beløp = parseInt(BELØP, it)
            val antallBarn = parseInt(ANTALL_BARN, it)
            val barn = barnPåIndex.take(antallBarn)
            utgiftsperioder.add(
                UtgiftsperiodeDto(
                    årMånedFra = fraÅrMåned,
                    årMånedTil = tilÅrMåned,
                    periode = Månedsperiode(fraÅrMåned, tilÅrMåned),
                    barn = barn,
                    utgifter = beløp,
                    sanksjonsårsak = null,
                    periodetype = periodetype,
                    aktivitetstype = aktivitetstype,
                ),
            )
        }
    }

    @Gitt("kontantstøtteperioder")
    fun kontantstøtteperioder(dataTable: DataTable) {
        dataTable.asMaps().map {
            val fraÅrMåned = parseÅrMåned(FRA_MND, it)
            val tilÅrMåned = parseÅrMåned(TIL_OG_MED_MND, it)
            val beløp = it["Beløp"]!!.toInt()
            kontantStøtteperioder.add(PeriodeMedBeløpDto(fraÅrMåned, tilÅrMåned, Månedsperiode(fraÅrMåned, tilÅrMåned), beløp))
        }
    }

    @Gitt("tilleggsstønadsperioder")
    fun tilleggsstønadsperioder(dataTable: DataTable) {
        dataTable.asMaps().map {
            val fraÅrMåned = parseÅrMåned(FRA_MND, it)
            val tilÅrMåned = parseÅrMåned(TIL_OG_MED_MND, it)
            val beløp = it["Beløp"]!!.toInt()
            tilleggsstønadPerioder.add(PeriodeMedBeløpDto(fraÅrMåned, tilÅrMåned, Månedsperiode(fraÅrMåned, tilÅrMåned), beløp))
        }
    }

    @Gitt("følgende fødselsdato {string} for barn")
    fun fødselsdato_og_dato_for_sjekk_på_fullført_fjerdetrinn(fødselsdato: String) {
        gittFødselsdato = parseDato(fødselsdato)
    }

    @Når("sjekk på om barn har fullført fjerde skoletrinn utføres den {string}")
    fun sjekk_har_fullført_fjerde_skoletrinn(datoSjekkUtføres: String) {
        datoForKjøring = parseDato(datoSjekkUtføres)
        resultatHarFullførtFjerdetrinn = AlderPåBarnRegelUtil.harFullførtFjerdetrinn(gittFødselsdato, parseDato(datoSjekkUtføres))
    }

    @Når("vi beregner perioder med barnetilsyn")
    fun `vi beregner perioder med barnetilsyn`() {
        beregnYtelseBarnetilsynResultat.addAll(
            beregningBarnetilsynService.beregnYtelseBarnetilsyn(
                utgiftsperioder = utgiftsperioder,
                kontantstøttePerioder = kontantStøtteperioder,
                tilleggsstønadsperioder = tilleggsstønadPerioder,
            ),
        )
    }

    @Så("forventer vi følgende perioder")
    fun `forventer vi barnetilsyn periodebeløp`(dataTable: DataTable) {
        val forventet =
            dataTable.asMaps().map {
                val beløp = parseInt(BELØP, it)
                val fraÅrMåned = parseÅrMåned(FRA_MND, it)
                val tilÅrMåned = parseÅrMåned(TIL_OG_MED_MND, it)
                ForventetPeriode(beløp, fraÅrMåned, tilÅrMåned)
            }
        assertThat(beregnYtelseBarnetilsynResultat).size().isEqualTo(forventet.size)
        val sortedResultat = beregnYtelseBarnetilsynResultat.sortedBy { it.periode.fom }
        val sortetForventet = forventet.sortedBy { it.fraÅrMåned }
        assertThat(sortedResultat.first().deprecatedPeriode.fradato).isEqualTo(sortetForventet.first().fraÅrMåned.atDay(1))
        assertThat(sortedResultat.last().deprecatedPeriode.fradato).isEqualTo(sortetForventet.last().fraÅrMåned.atDay(1))
        assertThat(sortedResultat.first().periode.fomDato).isEqualTo(sortetForventet.first().fraÅrMåned.atDay(1))
        assertThat(sortedResultat.last().periode.fomDato).isEqualTo(sortetForventet.last().fraÅrMåned.atDay(1))

        sortedResultat.forEachIndexed { idx, it ->
            assertThat(it.deprecatedPeriode.fradato).isEqualTo(sortetForventet.get(idx).fraÅrMåned.atDay(1))
            assertThat(it.deprecatedPeriode.tildato).isEqualTo(sortetForventet.get(idx).tilÅrMåned.atEndOfMonth())
            assertThat(it.periode.fomDato).isEqualTo(sortetForventet.get(idx).fraÅrMåned.atDay(1))
            assertThat(it.periode.tomDato).isEqualTo(sortetForventet.get(idx).tilÅrMåned.atEndOfMonth())
            assertThat(it.beløp).isEqualTo(sortetForventet.get(idx).beløp)
        }
    }

    @Så("forventer vi følgende perioder med riktig grunnlagsdata")
    fun `forventer vi følgende perioder med riktig grunnlagsdata`(dataTable: DataTable) {
        val forventet = hentUtForventet(dataTable)
        sjekkAtAlleFelterErSomForventet(forventet)
        assertThat(beregnYtelseBarnetilsynResultat).size().isEqualTo(forventet.size)
    }

    @Så("forvent resultat {string}")
    fun forvent_resultat_fjerde_skoletrinn(forventetResultat: String) {
        assertThat(resultatHarFullførtFjerdetrinn).isEqualTo(parseBoolean(forventetResultat))
    }

    private fun sjekkAtAlleFelterErSomForventet(forventet: List<ForventetPeriodeMedGrunnlag>) {
        val sortedResultat = beregnYtelseBarnetilsynResultat.sortedBy { it.periode.fom }
        val sortetForventet = forventet.sortedBy { it.fraÅrMåned }

        sortedResultat.forEachIndexed { idx, it ->
            assertAllefelterErSomForventet(it, sortetForventet, idx)
        }
    }

    private fun hentUtForventet(dataTable: DataTable) =
        dataTable.asMaps().map {
            val beløp = it["Beløp"]!!.toInt()
            val fraÅrMåned = parseÅrMåned(FRA_MND, it)
            val tilÅrMåned = parseÅrMåned(TIL_OG_MED_MND, it)
            val harKontantstøtte = parseBooleanJaIsTrue(HAR_KONTANTSTØTTE, it)
            val harTilleggsstønad = parseBooleanJaIsTrue(HAR_TILLEGGSSTØNAD, it)
            val antallBarn = it["Antall barn"]!!.toInt()
            ForventetPeriodeMedGrunnlag(beløp, fraÅrMåned, tilÅrMåned, harKontantstøtte, harTilleggsstønad, antallBarn)
        }

    private fun assertAllefelterErSomForventet(
        it: BeløpsperiodeBarnetilsynDto,
        sortetForventet: List<ForventetPeriodeMedGrunnlag>,
        idx: Int,
    ) {
        assertThat(it.deprecatedPeriode.fradato).isEqualTo(sortetForventet.get(idx).fraÅrMåned.atDay(1))
        assertThat(it.deprecatedPeriode.tildato).isEqualTo(sortetForventet.get(idx).tilÅrMåned.atEndOfMonth())
        assertThat(it.periode.fomDato).isEqualTo(sortetForventet.get(idx).fraÅrMåned.atDay(1))
        assertThat(it.periode.tomDato).isEqualTo(sortetForventet.get(idx).tilÅrMåned.atEndOfMonth())
        assertThat(it.beløp).isEqualTo(sortetForventet.get(idx).beløp)
        assertThat(it.beregningsgrunnlag.antallBarn).isEqualTo(sortetForventet.get(idx).antallBarn)
        when (sortetForventet.get(idx).harKontantstøtte) {
            true -> assertThat(it.beregningsgrunnlag.kontantstøttebeløp).isGreaterThan(ZERO)
            false -> assertThat(it.beregningsgrunnlag.kontantstøttebeløp).isEqualByComparingTo(ZERO)
        }

        when (sortetForventet.get(idx).harTilleggsstønad) {
            true -> assertThat(it.beregningsgrunnlag.tilleggsstønadsbeløp).isGreaterThan(ZERO)
            false -> assertThat(it.beregningsgrunnlag.tilleggsstønadsbeløp).isEqualByComparingTo(ZERO)
        }
    }

    data class ForventetPeriode(
        val beløp: Int,
        val fraÅrMåned: YearMonth,
        val tilÅrMåned: YearMonth,
    )

    data class ForventetPeriodeMedGrunnlag(
        val beløp: Int,
        val fraÅrMåned: YearMonth,
        val tilÅrMåned: YearMonth,
        val harKontantstøtte: Boolean,
        val harTilleggsstønad: Boolean,
        val antallBarn: Int,
    )
}
