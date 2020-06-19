package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.gui.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.gui.dto.AnnenForelder
import no.nav.familie.ef.sak.api.gui.dto.Barn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.kontrakter.ef.søknad.Søknad
import no.nav.familie.kontrakter.ef.søknad.Barn as Søknadsbarn

object AleneomsorgMapper {

    fun tilDto(pdlSøker: PdlSøker,
               pdlBarn: Map<String, PdlBarn>,
               barneforeldre: Map<String, PdlAnnenForelder>,
               søknad: Søknad): Aleneomsorg {


        val alleBarn: List<Pair<Søknadsbarn?, PdlBarn?>> = lagUnionAvSøknadsbarnOgRegisterBarn(søknad.barn.verdi, pdlBarn)

        Aleneomsorg(
                tilBarn(pdlBarn,
                        pdlSøker,
                        barneforeldre,
                        søknad.barn.verdi)
        )


    }

    private fun lagUnionAvSøknadsbarnOgRegisterBarn(søknadsbarn: List<Søknadsbarn>,
                                                    pdlBarnMap: Map<String, PdlBarn>): List<Pair<Søknadsbarn?, PdlBarn?>> {

        val søknadsbarnMatchetTilPdlBarn =
                søknadsbarn.map {
                    it to pdlBarnMap[it.fødselsnummer?.verdi?.verdi]?.let { pdlBarn ->
                        it.fødselsnummer?.verdi?.verdi to pdlBarn
                    }
                }

        val pdlBarnMatchetTilSøknadsbarn =
                pdlBarnMap.map { entry ->
                    søknadsbarn.firstOrNull { it.fødselsnummer?.verdi?.verdi == entry.key } to entry
                }

        val registerbarnIkkeISøknad = pdlBarnMatchetTilSøknadsbarn.filter { it.first == null }
        val søknadsbarnIkkeIRegister = søknadsbarnMatchetTilPdlBarn.filter { it.second == null }

    }

    private fun tilBarn(pdlBarnMap: Map<String, PdlBarn>,
                        pdlSøker: PdlSøker,
                        barneforeldre: Map<String, PdlAnnenForelder>,
                        søknadsbarn: List<Søknadsbarn>): List<Barn> {


        søknadsbarn.map { tilBarn(it, pdlBarnMap[it.fødselsnummer?.verdi?.verdi], barneforeldre) }

        val pdlBarn = pdlBarnMap.value

        val søknadBarn =


                søknadBarn.annenForelder?.verdi?.kanIkkeOppgiAnnenForelderFar

        Barn(pdlBarn.navn.gjeldende().visningsnavn(),
             pdlBarnMap.key,
             pdlBarn.fødsel.first().fødselsdato,
             søknadsbarn.folkeregisterbarn.verdi.tilAnnenForelder(pdlBarn,
                                                                  barneforeldre,
                                                                  søknadsbarn)
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

    private fun tilAnnenForelder(pdlBarn: PdlBarn, barneforeldre: Map<String, PdlAnnenForelder>): AnnenForelder? {

        if (pdlBarn.familierelasjoner.isEmpty()) {
            return null
        }


    }

}