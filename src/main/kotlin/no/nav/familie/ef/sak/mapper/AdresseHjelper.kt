package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AdresseDto
import no.nav.familie.ef.sak.api.dto.AdresseType
import no.nav.familie.ef.sak.domene.BarnMedIdent
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import java.time.LocalDate

object AdresseHjelper {

    fun sorterAdresser(adresser: List<AdresseDto>): List<AdresseDto> {
        val (historiskeAdresser, aktiveAdresser) = adresser
                .sortedWith(compareByDescending<AdresseDto> { it.gyldigFraOgMed ?: LocalDate.MAX }.thenBy(AdresseDto::type))
                .partition { it.gyldigTilOgMed != null }

        val (bostedsadresse, aktivUtenBostedsadresse) = aktiveAdresser.partition { it.type == AdresseType.BOSTEDADRESSE }

        return bostedsadresse + aktivUtenBostedsadresse + historiskeAdresser
    }

    fun borPåSammeAdresse(barn: BarnMedIdent, bostedsadresserForelder: List<Bostedsadresse>): Boolean {

        if (harDeltBosted(barn)) {
            return false
        }

        val gjeldendeBostedsadresseForelder = bostedsadresserForelder.gjeldende()

        return barn.bostedsadresse.gjeldende()?.let { adresseBarn ->
            return adresseBarn.matrikkelId()?.let { matrikkelId ->
                return matrikkelId == gjeldendeBostedsadresseForelder?.matrikkelId()
            } ?: adresseBarn.vegadresse != null && adresseBarn.vegadresse == gjeldendeBostedsadresseForelder?.vegadresse
        } ?: false
    }

    private fun harDeltBosted(barn: BarnMedIdent): Boolean {
        return barn.deltBosted.any {
            it.startdatoForKontrakt.isBefore(LocalDate.now())
            && (it.sluttdatoForKontrakt == null || it.sluttdatoForKontrakt.isAfter(LocalDate.now()))
        }
    }

}