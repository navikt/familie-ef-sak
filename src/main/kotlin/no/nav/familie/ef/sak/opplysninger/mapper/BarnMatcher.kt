package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Barn
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import java.time.LocalDate
import kotlin.math.abs

object BarnMatcher {

    fun kobleSøknadsbarnOgRegisterBarn(søknadsbarn: Set<Barn>, barn: List<BarnMedIdent>): List<MatchetBarn> {
        val barnMap = barn.associateBy { it.personIdent }
        val søknadsbarnMedFnrMatchetTilPdlBarn =
                søknadsbarn.map {
                    val firstOrNull = barnMap.entries.firstOrNull { entry -> it.fødselsnummer == entry.key }
                    MatchetBarn(firstOrNull?.key, firstOrNull?.value, it)
                }

        val pdlBarnIkkeISøknad =
                barnMap.filter { entry ->
                    søknadsbarn.firstOrNull { it.fødselsnummer == entry.key } == null
                }.toMutableMap()

        return søknadsbarnMedFnrMatchetTilPdlBarn.map {
            if (it.barn != null) {
                it
            } else {
                val barnForsøktMatchetPåFødselsdato = forsøkMatchPåFødselsdato(it, pdlBarnIkkeISøknad)
                if (barnForsøktMatchetPåFødselsdato.fødselsnummer != null) {
                    pdlBarnIkkeISøknad.remove(barnForsøktMatchetPåFødselsdato.fødselsnummer)
                }
                barnForsøktMatchetPåFødselsdato
            }
        }
    }

    private fun forsøkMatchPåFødselsdato(barn: MatchetBarn,
                                         pdlBarnIkkeISøknad: Map<String, BarnMedIdent>): MatchetBarn {

        val fødselTermindato = barn.søknadsbarn.fødselTermindato ?: return barn
        val uke20 = fødselTermindato.minusWeeks(20)
        val uke44 = fødselTermindato.plusWeeks(4)

        val nærmesteMatch = pdlBarnIkkeISøknad.entries.filter {
            val fødselsnummer = Fødselsnummer(it.key)
            val fødselsdato = fødselsnummer.fødselsdato
            fødselsdato.isBefore(uke44) and fødselsdato.isAfter(uke20)
        }.minByOrNull {
            val epochDayForFødsel = Fødselsnummer(it.key).fødselsdato.toEpochDay()
            val epochDayTermindato = fødselTermindato.toEpochDay()
            abs(epochDayForFødsel - epochDayTermindato)
        } ?: return barn

        return barn.copy(fødselsnummer = nærmesteMatch.key, barn = nærmesteMatch.value)

    }

    fun forsøkMatchPåFødselsdato(fødselTermindato: LocalDate, pdlBarnIkkeISøknad: List<BarnMedIdent>): BarnMedIdent? {
        val uke20 = fødselTermindato.minusWeeks(20)
        val uke44 = fødselTermindato.plusWeeks(4)

        val nærmesteMatch = pdlBarnIkkeISøknad.filter {
            val fødselsdato = it.fødsel.gjeldende().fødselsdato ?: Fødselsnummer(it.personIdent).fødselsdato
            fødselsdato.isBefore(uke44) and fødselsdato.isAfter(uke20)
        }.minByOrNull {
            val epochDayForFødsel =
                    it.fødsel.gjeldende().fødselsdato?.toEpochDay() ?: Fødselsnummer(it.personIdent).fødselsdato.toEpochDay()
            val epochDayTermindato = fødselTermindato.toEpochDay()
            abs(epochDayForFødsel - epochDayTermindato)
        }

        return nærmesteMatch
    }

}

data class MatchetBarn(val fødselsnummer: String?, val barn: BarnMedIdent?, val søknadsbarn: Barn)
