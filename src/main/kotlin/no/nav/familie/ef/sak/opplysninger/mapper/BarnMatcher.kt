package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.BarnMedFødselsdatoDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
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
        val besteMatch =
            finnBesteMatchPåFødselsnummerForTermindato(
                pdlBarnIkkeISøknad.map {
                    BarnMedFødselsdatoDto(
                        it.key,
                        it.value.fødsel
                            .first()
                            .fødselsdato,
                    )
                },
                fødselTermindato,
            )?.barnIdent
        return pdlBarnIkkeISøknad[besteMatch]
    }
}

data class MatchetBehandlingBarn(
    val fødselsnummer: String?,
    val barn: BarnMedIdent?,
    val behandlingBarn: BehandlingBarn,
)

fun finnBesteMatchPåFødselsnummerForTermindato(
    barn: List<BarnMedFødselsdatoDto>,
    termindato: LocalDate,
): BarnMedFødselsdatoDto? {
    val uke20 = termindato.minusWeeks(20)
    val uke44 = termindato.plusWeeks(4)

    return barn
        .filter {
            it.fødselsdato != null && (it.fødselsdato.isBefore(uke44) and it.fødselsdato.isAfter(uke20))
        }.minByOrNull { akuteltBarn ->
            abs(akuteltBarn.fødselsdato!!.toEpochDay() - termindato.toEpochDay())
        }
}
