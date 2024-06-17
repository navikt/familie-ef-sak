package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.kontrakter.felles.Fødselsnummer
import java.time.LocalDate
import kotlin.math.abs

object BarnMatcher {
    fun kobleBehandlingBarnOgRegisterBarn(
        behandlingBarn: List<BehandlingBarn>,
        grunnlagsbarn: List<BarnMedIdent>,
    ): List<MatchetBehandlingBarn> {
        val grunnlagsbarnPåIdent = grunnlagsbarn.associateBy { it.personIdent }
        val behandlingBarnFnrMatchetTilPdlBarn =
            behandlingBarn.map {
                val matchetBarnPåIdent = grunnlagsbarnPåIdent[it.personIdent]
                MatchetBehandlingBarn(matchetBarnPåIdent?.personIdent, matchetBarnPåIdent, it)
            }

        val pdlBarnIkkeIBehandlingBarn =
            grunnlagsbarnPåIdent.filter { entry -> behandlingBarn.none { it.personIdent == entry.key } }.toMutableMap()

        return behandlingBarnFnrMatchetTilPdlBarn.map {
            if (it.barn != null) {
                it
            } else {
                val barnForsøktMatchetPåFødselsdato = forsøkMatchPåFødselsdato(it, pdlBarnIkkeIBehandlingBarn)
                if (barnForsøktMatchetPåFødselsdato.fødselsnummer != null) {
                    pdlBarnIkkeIBehandlingBarn.remove(barnForsøktMatchetPåFødselsdato.fødselsnummer)
                }
                barnForsøktMatchetPåFødselsdato
            }
        }
    }

    private fun forsøkMatchPåFødselsdato(
        barn: MatchetBehandlingBarn,
        pdlBarnIkkeISøknad: Map<String, BarnMedIdent>,
    ): MatchetBehandlingBarn {
        val fødselTermindato = barn.behandlingBarn.fødselTermindato ?: return barn
        val nærmesteMatchBarnMedIdent = nærmesteMatch(pdlBarnIkkeISøknad, fødselTermindato) ?: return barn

        return barn.copy(fødselsnummer = nærmesteMatchBarnMedIdent.personIdent, barn = nærmesteMatchBarnMedIdent)
    }

    private fun nærmesteMatch(
        pdlBarnIkkeISøknad: Map<String, BarnMedIdent>,
        fødselTermindato: LocalDate,
    ): BarnMedIdent? {
        val besteMatch = finnBesteMatchPåFødselsnummerForTermindato(pdlBarnIkkeISøknad.map { it.key }, fødselTermindato)
        return pdlBarnIkkeISøknad[besteMatch]
    }
}

data class MatchetBehandlingBarn(
    val fødselsnummer: String?,
    val barn: BarnMedIdent?,
    val behandlingBarn: BehandlingBarn,
)

fun finnBesteMatchPåFødselsnummerForTermindato(
    fødselsnumre: List<String>,
    termindato: LocalDate,
): String? {
    val uke20 = termindato.minusWeeks(20)
    val uke44 = termindato.plusWeeks(4)

    return fødselsnumre
        .filter {
            val fødselsnummer = Fødselsnummer(it)
            val fødselsdato = fødselsnummer.fødselsdato
            fødselsdato.isBefore(uke44) and fødselsdato.isAfter(uke20)
        }.minByOrNull {
            val epochDayForFødsel = Fødselsnummer(it).fødselsdato.toEpochDay()
            val epochDayTermindato = termindato.toEpochDay()
            abs(epochDayForFødsel - epochDayTermindato)
        }
}
