package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import java.time.LocalDate

object AdresseHjelper {

    fun sorterAdresser(adresser: List<AdresseDto>): List<AdresseDto> {
        return adresser.sortedWith(compareBy<AdresseDto> { it.type.rekkefølge }
                                           .thenByDescending { it.angittFlyttedato ?: it.gyldigFraOgMed })
    }

    fun borPåSammeAdresse(barn: BarnMedIdent, bostedsadresserForelder: List<Bostedsadresse>): Boolean {

        if (harDeltBosted(barn)) {
            return false
        }

        return sammeMatrikkeladresse(bostedsadresserForelder.gjeldende(), barn.bostedsadresse.gjeldende())
               || sammeVegadresse(bostedsadresserForelder.gjeldende(), barn.bostedsadresse.gjeldende())
    }

    private fun sammeMatrikkeladresse(bostedsadresseForelder: Bostedsadresse?, bostedsadresseBarn: Bostedsadresse?): Boolean {
        return bostedsadresseBarn?.matrikkelId != null && bostedsadresseForelder?.matrikkelId != null
               && bostedsadresseBarn.matrikkelId == bostedsadresseForelder.matrikkelId
               && bostedsadresseBarn.bruksenhetsnummer == bostedsadresseForelder.bruksenhetsnummer
    }

    private fun sammeVegadresse(bostedsadresseForelder: Bostedsadresse?, bostedsadresseBarn: Bostedsadresse?): Boolean {
        return bostedsadresseBarn?.vegadresse != null && bostedsadresseForelder?.vegadresse != null
               && bostedsadresseBarn.vegadresse == bostedsadresseForelder.vegadresse
    }

    private fun harDeltBosted(barn: BarnMedIdent): Boolean {
        return barn.deltBosted.any {
            it.startdatoForKontrakt.isBefore(LocalDate.now())
            && (it.sluttdatoForKontrakt == null || it.sluttdatoForKontrakt.isAfter(LocalDate.now()))
        }
    }

}