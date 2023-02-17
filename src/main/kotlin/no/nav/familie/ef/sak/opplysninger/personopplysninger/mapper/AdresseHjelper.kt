package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import java.time.LocalDate

object AdresseHjelper {

    fun sorterAdresser(adresser: List<AdresseDto>): List<AdresseDto> {
        return adresser.sortedWith(
            compareBy<AdresseDto> { it.type.rekkefølge }
                .thenByDescending { it.erGjeldende }
                .thenByDescending { it.angittFlyttedato ?: it.gyldigFraOgMed }
        )
    }

    fun borPåSammeAdresse(barn: BarnMedIdent, bostedsadresserForelder: List<Bostedsadresse>): Boolean {
        if (harDeltBostedNå(barn)) {
            return true
        }
        return sammeMatrikkeladresse(bostedsadresserForelder.gjeldende(), barn.bostedsadresse.gjeldende()) ||
            sammeVegadresse(bostedsadresserForelder.gjeldende(), barn.bostedsadresse.gjeldende())
    }

    private fun sammeMatrikkeladresse(
        bostedsadresseForelder: Bostedsadresse?,
        bostedsadresseBarn: Bostedsadresse?
    ): Boolean {
        return bostedsadresseBarn?.matrikkelId != null && bostedsadresseForelder?.matrikkelId != null &&
            bostedsadresseBarn.matrikkelId == bostedsadresseForelder.matrikkelId &&
            bostedsadresseBarn.bruksenhetsnummer == bostedsadresseForelder.bruksenhetsnummer
    }

    private fun sammeVegadresse(bostedsadresseForelder: Bostedsadresse?, bostedsadresseBarn: Bostedsadresse?): Boolean {
        return bostedsadresseBarn?.vegadresse != null && bostedsadresseForelder?.vegadresse != null &&
            bostedsadresseBarn.vegadresse == bostedsadresseForelder.vegadresse
    }

    fun harDeltBostedNå(barn: BarnMedIdent): Boolean {
        val gjeldende = barn.deltBosted.gjeldende() ?: return false
        val nå = LocalDate.now()
        return gjeldende.startdatoForKontrakt.isEqualOrBefore(nå) &&
            (gjeldende.sluttdatoForKontrakt == null || gjeldende.sluttdatoForKontrakt.isEqualOrAfter(nå))
    }
}
