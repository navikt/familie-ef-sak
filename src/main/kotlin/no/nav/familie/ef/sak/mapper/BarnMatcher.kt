package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.repository.domain.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import kotlin.math.abs

object BarnMatcher {

    fun kobleSøknadsbarnOgRegisterBarn(søknadsbarn: Set<Barn>, pdlBarnMap: Map<String, PdlBarn>): List<MatchetBarn> {

        val søknadsbarnMedFnrMatchetTilPdlBarn =
                søknadsbarn.map {
                    val firstOrNull =
                            pdlBarnMap.entries.firstOrNull { entry -> it.fødselsnummer == entry.key && !it.lagtTilManuelt }
                    MatchetBarn(firstOrNull?.key, firstOrNull?.value, it)
                }

        val pdlBarnIkkeISøknad =
                pdlBarnMap.filter { entry ->
                    søknadsbarn.firstOrNull { it.fødselsnummer == entry.key } == null
                }.toMutableMap()


        return søknadsbarnMedFnrMatchetTilPdlBarn.map {
            if (it.pdlBarn != null) {
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
                                         pdlBarnIkkeISøknad: Map<String, PdlBarn>): MatchetBarn {

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

        return barn.copy(fødselsnummer = nærmesteMatch.key, pdlBarn = nærmesteMatch.value)

    }

}

data class MatchetBarn(val fødselsnummer: String?, val pdlBarn: PdlBarn?, val søknadsbarn: Barn)
