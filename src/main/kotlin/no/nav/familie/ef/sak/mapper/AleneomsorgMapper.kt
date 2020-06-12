package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.gui.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.gui.dto.AnnenForelder
import no.nav.familie.ef.sak.api.gui.dto.Barn
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.kontrakter.ef.søknad.Søknad

object AleneomsorgMapper {

    fun tilDto(pdlSøker: PdlSøker,
               barn: Map<String, PdlBarn>,
               barneforeldre: Map<String, PdlAnnenForelder>,
               søknad: Søknad): Aleneomsorg {


        val alleBarn: Map<>lagUnionAvSøknadsbarnOgRegisterBarn(søknad, barn)

        Aleneomsorg(barn.map {
            tilBarn(it,
                    pdlSøker,
                    barneforeldre,
                    søknad)
        })


    }

    private fun tilBarn(pdlBarnMap: Map.Entry<String, PdlBarn>,
                        pdlSøker: PdlSøker,
                        barneforeldre: Map<String, PdlAnnenForelder>,
                        søknad: Søknad): Barn {
        val pdlBarn = pdlBarnMap.value

        val søknadBarn = søknad.folkeregisterbarn?.verdi?.find { it.fødselsnummer.verdi.verdi == pdlBarnMap.key  }



        søknadBarn.annenForelder?.verdi?.kanIkkeOppgiAnnenForelderFar

        Barn(pdlBarn.navn.gjeldende().visningsnavn(),
                                                              pdlBarnMap.key,
                                                              pdlBarn.fødsel.first().fødselsdato,
                                                              søknad.folkeregisterbarn.verdi.tilAnnenForelder(pdlBarn,
                                                                                                              barneforeldre,
                                                                                                              søknad)

    }

    private fun tilAnnenForelder(pdlBarn: PdlBarn, barneforeldre: Map<String, PdlAnnenForelder>): AnnenForelder? {

        if (pdlBarn.familierelasjoner.isEmpty()) {
            return null
        }


    }

}