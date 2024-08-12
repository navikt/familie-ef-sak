package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import java.time.LocalDate

object AdresseHjelper {
    fun sorterAdresser(adresser: List<AdresseDto>): List<AdresseDto> =
        adresser.sortedWith(
            compareBy<AdresseDto> { it.type.rekkefølge }
                .thenByDescending { it.erGjeldende }
                .thenByDescending { it.angittFlyttedato ?: it.gyldigFraOgMed },
        )

    fun harRegistrertSammeBostedsadresseSomForelder(
        barn: BarnMedIdent,
        bostedsadresserForelder: List<Bostedsadresse>,
    ): Boolean =
        sammeMatrikkeladresse(bostedsadresserForelder.gjeldende(), barn.bostedsadresse.gjeldende()) ||
            sammeVegadresse(bostedsadresserForelder.gjeldende(), barn.bostedsadresse.gjeldende())

    private fun sammeMatrikkeladresse(
        bostedsadresseForelder: Bostedsadresse?,
        bostedsadresseBarn: Bostedsadresse?,
    ): Boolean =
        bostedsadresseBarn?.matrikkelId != null &&
            bostedsadresseForelder?.matrikkelId != null &&
            bostedsadresseBarn.matrikkelId == bostedsadresseForelder.matrikkelId &&
            bostedsadresseBarn.bruksenhetsnummer ?: "" == bostedsadresseForelder.bruksenhetsnummer ?: ""

    private fun sammeVegadresse(
        bostedsadresseForelder: Bostedsadresse?,
        bostedsadresseBarn: Bostedsadresse?,
    ): Boolean =
        bostedsadresseBarn?.vegadresse != null &&
            bostedsadresseForelder?.vegadresse != null &&
            bostedsadresseBarn.vegadresse.erSammeVegadresse(bostedsadresseForelder.vegadresse)

    fun harDeltBosted(
        barn: BarnMedIdent?,
        dato: LocalDate,
    ): Boolean {
        if (barn == null || barn.erOver18År()) {
            return false
        }

        val gjeldende = barn.deltBosted.gjeldende() ?: return false
        return gjeldende.startdatoForKontrakt <= dato &&
            (gjeldende.sluttdatoForKontrakt == null || gjeldende.sluttdatoForKontrakt >= dato)
    }

    private fun BarnMedIdent.erOver18År(): Boolean = !fødsel.gjeldende().erUnder18År()
}
