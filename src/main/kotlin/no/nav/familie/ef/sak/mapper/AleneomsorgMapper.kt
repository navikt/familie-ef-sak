package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.gui.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.gui.dto.AnnenForelder
import no.nav.familie.ef.sak.api.gui.dto.Barn
import no.nav.familie.ef.sak.api.gui.dto.DeltBosted
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.kontrakter.ef.søknad.Søknad
import no.nav.familie.kontrakter.ef.søknad.Barn as Søknadsbarn

object AleneomsorgMapper {

    fun tilDto(pdlSøker: PdlSøker,
               pdlBarn: Map<String, PdlBarn>,
               barneforeldre: Map<String, PdlAnnenForelder>,
               søknad: Søknad): Aleneomsorg {


        val alleBarn = lagUnionAvSøknadsbarnOgRegisterBarn(søknad.barn.verdi, pdlBarn)

        Aleneomsorg(
                alleBarn.map {
                    tilBarn(it, barneforeldre)
                }
        )


    }

    private fun lagUnionAvSøknadsbarnOgRegisterBarn(søknadsbarn: List<Søknadsbarn>, pdlBarnMap: Map<String, PdlBarn>)
            : List<Pair<Søknadsbarn?, Map.Entry<String, PdlBarn>?>> {

        val søknadsbarnMatchetTilPdlBarn =
                søknadsbarn.map {
                    it to pdlBarnMap.entries.firstOrNull { entry -> it.fødselsnummer?.verdi?.verdi == entry.key }
                }

        val pdlBarnIkkeISøknad =
                pdlBarnMap.filter { entry ->
                    søknadsbarn.firstOrNull { it.fødselsnummer?.verdi?.verdi == entry.key } == null
                }.map { entry ->
                    søknadsbarn.firstOrNull { it.fødselsnummer?.verdi?.verdi == entry.key } to entry
                }

        return søknadsbarnMatchetTilPdlBarn + pdlBarnIkkeISøknad
    }

    private fun tilBarn(it: Pair<no.nav.familie.kontrakter.ef.søknad.Barn?, Map.Entry<String, PdlBarn>?>,
                        barneforeldre: Map<String, PdlAnnenForelder>): Barn {

        val navn = it.second?.value?.navn?.firstOrNull()?.visningsnavn() ?: it.first?.navn?.verdi ?: "Ubestemt"
        val fødselsnummer = it.second?.key ?: it.first?.fødselsnummer?.verdi?.verdi
        val termindatoFødselsdato = it.second?.value?.fødsel?.firstOrNull()?.fødselsdato ?: it.first?.fødselTermindato?.verdi
        val begrunnelseIkkeOppgittAnnenForelder = it.first?.annenForelder?.verdi?.ikkeOppgittAnnenForelderBegrunnelse?.verdi
        val annenForelder = tilAnnenForelder(it, barneforeldre)
        val aleneomsorg = it.first?.
        val søkersRelasjonTilBarnet =
        val skalBoBorHosSøker = it.first?.harSkalHaSammeAdresse?.verdi
        val deltBosted = tilDeltBosted(it.second?.value?.deltBosted?.firstOrNull())
        val fraRegister = it.second != null

        return Barn(navn,
                    fødselsnummer,
                    termindatoFødselsdato,
                    begrunnelseIkkeOppgittAnnenForelder,
                    annenForelder,
                    aleneomsorg,
                    søkersRelasjonTilBarnet,
                    skalBoBorHosSøker,
                    deltBosted,
                    fraRegister)
    }

    private fun tilDeltBosted(firstOrNull: no.nav.familie.ef.sak.integration.dto.pdl.DeltBosted?): DeltBosted {


    }

    private fun finnMatchendePdlBarn(søknadsbarn: Søknadsbarn, pdlBarnMap: Map<String, PdlBarn>): PdlBarn? {
        // TODO ev. logikk for å matche barn uten fødselsnummer fra søknad med nyfødte barn fra pdl.
        if (søknadsbarn.fødselsnummer == null) {
            sø
        }


        pdlBarnMap[søknadsbarn.fødselsnummer?.verdi?.verdi]
    }

    private fun tilBarn(pdlBarnMap: Søknadsbarn, pdlBarn: PdlBarn?, barneforeldre: Map<String, PdlAnnenForelder>): List<Barn> {
        TODO("Not yet implemented")
    }

    private fun tilAnnenForelder(pdlBarn: Pair<no.nav.familie.kontrakter.ef.søknad.Barn?, Map.Entry<String, PdlBarn>?>, barneforeldre: Map<String, PdlAnnenForelder>): AnnenForelder? {

        if (pdlBarn.familierelasjoner.isEmpty()) {
            return null
        }


    }

}