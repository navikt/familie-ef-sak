package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import java.time.LocalDate
import kotlin.math.abs


object BarnMatcher {

    fun kobleBehandlingBarnOgRegisterBarn(behandlingBarn: List<BehandlingBarn>,
                                          barn: List<BarnMedIdent>): List<MatchetBehandlingBarn> {
        val barnMap = barn.associateBy { it.personIdent }
        val behandlingBarnFnrMatchetTilPdlBarn = behandlingBarn.map {
            val firstOrNull = barnMap.entries.firstOrNull { entry -> it.personIdent == entry.key }
            MatchetBehandlingBarn(firstOrNull?.key, firstOrNull?.value, it)
        }

        val pdlBarnIkkeIBehandlingBarn =
                barnMap.filter { entry -> behandlingBarn.none { it.personIdent == entry.key } }.toMutableMap()

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

    private fun forsøkMatchPåFødselsdato(barn: MatchetBehandlingBarn,
                                         pdlBarnIkkeISøknad: Map<String, BarnMedIdent>): MatchetBehandlingBarn {

        val fødselTermindato = barn.behandlingBarn.fødselTermindato ?: return barn
        val nærmesteMatch = nærmesteMatch(pdlBarnIkkeISøknad, fødselTermindato) ?: return barn

        return barn.copy(fødselsnummer = nærmesteMatch.key, barn = nærmesteMatch.value)

    }

    private fun nærmesteMatch(pdlBarnIkkeISøknad: Map<String, BarnMedIdent>,
                              fødselTermindato: LocalDate): Map.Entry<String, BarnMedIdent>? {
        val uke20 = fødselTermindato.minusWeeks(20)
        val uke44 = fødselTermindato.plusWeeks(4)

        return pdlBarnIkkeISøknad.entries.filter {
            val fødselsnummer = Fødselsnummer(it.key)
            val fødselsdato = fødselsnummer.fødselsdato
            fødselsdato.isBefore(uke44) and fødselsdato.isAfter(uke20)
        }.minByOrNull {
            val epochDayForFødsel = Fødselsnummer(it.key).fødselsdato.toEpochDay()
            val epochDayTermindato = fødselTermindato.toEpochDay()
            abs(epochDayForFødsel - epochDayTermindato)
        }
    }

    // TODO: Denne må støtte tvillinger
    fun forsøkMatchPåFødselsdato(fødselTermindato: LocalDate, barnFraPdlMenIkkeIBehandlingen: List<BarnMedIdent>): BarnMedIdent? {
        val uke20 = fødselTermindato.minusWeeks(20)
        val uke44 = fødselTermindato.plusWeeks(4)

        val nærmesteMatch = barnFraPdlMenIkkeIBehandlingen.filter {
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

data class MatchetBehandlingBarn(val fødselsnummer: String?,
                                 val barn: BarnMedIdent?,
                                 val behandlingBarn: BehandlingBarn)