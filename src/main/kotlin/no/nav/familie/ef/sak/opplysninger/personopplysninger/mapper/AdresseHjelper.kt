package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import java.time.LocalDate

object AdresseHjelper {

    fun sorterAdresser(adresser: List<AdresseDto>): List<AdresseDto> {
        return adresser.sortedWith(compareBy<AdresseDto> { it.type.rekkefølge }
                                           .thenByDescending { it.angittFlyttedato ?: it.gyldigFraOgMed ?: LocalDate.MAX })
    }

    fun borPåSammeAdresse(barn: BarnMedIdent, bostedsadresserForelder: List<Bostedsadresse>): Boolean {

        if (harDeltBosted(barn)) {
            return false
        }

        val gjeldendeBostedsadresseForelder = bostedsadresserForelder.gjeldende()

        return barn.bostedsadresse.gjeldende()?.let { adresseBarn ->
            return (adresseBarn.matrikkelId()?.let { matrikkelId ->
                return matrikkelId == gjeldendeBostedsadresseForelder?.matrikkelId()
            } ?: adresseBarn.vegadresse) != null && adresseBarn.vegadresse == gjeldendeBostedsadresseForelder?.vegadresse
        } ?: false
    }

    private fun harDeltBosted(barn: BarnMedIdent): Boolean {
        return barn.deltBosted.any {
            it.startdatoForKontrakt.isBefore(LocalDate.now())
            && (it.sluttdatoForKontrakt == null || it.sluttdatoForKontrakt.isAfter(LocalDate.now()))
        }
    }

}