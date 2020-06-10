package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AdresseDto
import no.nav.familie.ef.sak.api.dto.AdresseType
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.service.KodeverkService
import no.nav.familie.ef.sak.util.datoEllerIdag
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AdresseMapper(private val kodeverkService: KodeverkService) {

    fun tilAdresse(adresse: Oppholdsadresse): AdresseDto {
        val visningsadresse = tilFormatertAdresse(adresse, datoEllerIdag(adresse.oppholdsadressedato))
        return AdresseDto(visningsadresse = visningsadresse,
                          type = AdresseType.OPPHOLDSADRESSE,
                          gyldigFraOgMed = adresse.oppholdsadressedato,
                          gyldigTilOgMed = null)
    }

    fun tilAdresse(adresse: Kontaktadresse): AdresseDto {
        val type = when (adresse.type) {
            KontaktadresseType.INNLAND -> AdresseType.KONTAKTADRESSE
            KontaktadresseType.UTLAND -> AdresseType.KONTAKTADRESSE_UTLAND
        }
        return AdresseDto(visningsadresse = tilFormatertAdresse(adresse, datoEllerIdag(adresse.gyldigFraOgMed)),
                          type = type,
                          gyldigFraOgMed = adresse.gyldigFraOgMed,
                          gyldigTilOgMed = adresse.gyldigTilOgMed)
    }

    fun tilAdresse(adresse: Bostedsadresse): AdresseDto {
        return AdresseDto(visningsadresse = tilFormatertAdresse(adresse, datoEllerIdag(adresse.angittFlyttedato)),
                          type = AdresseType.BOSTEDADRESSE,
                          gyldigFraOgMed = adresse.angittFlyttedato,
                          gyldigTilOgMed = adresse.folkeregistermetadata.opphørstidspunkt?.toLocalDate())
    }

    private fun tilFormatertAdresse(bostedsadresse: Bostedsadresse, gjeldendeDato: LocalDate): String? {
        val adresse = bostedsadresse.vegadresse?.let { tilFormatertAdresse(it, gjeldendeDato) }
                      ?: bostedsadresse.ukjentBosted?.bostedskommune
        return join(coAdresse(bostedsadresse.coAdressenavn), adresse)
    }

    private fun tilFormatertAdresse(oppholdsadresse: Oppholdsadresse, gjeldendeDato: LocalDate): String? {
        val adresse = oppholdsadresse.vegadresse?.let { tilFormatertAdresse(it, gjeldendeDato) }
                      ?: oppholdsadresse.utenlandskAdresse?.let { tilFormatertAdresse(it, gjeldendeDato) }
        return join(coAdresse(oppholdsadresse.coAdressenavn), adresse)
    }

    private fun tilFormatertAdresse(kontaktadresse: Kontaktadresse, gjeldendeDato: LocalDate): String? {
        val adresse = when (kontaktadresse.type) {
            KontaktadresseType.INNLAND -> when {
                kontaktadresse.vegadresse != null -> tilFormatertAdresse(kontaktadresse.vegadresse, gjeldendeDato)
                kontaktadresse.postboksadresse != null -> tilFormatertAdresse(kontaktadresse.postboksadresse, gjeldendeDato)
                else -> kontaktadresse.postadresseIFrittFormat?.let { tilFormatertAdresse(it, gjeldendeDato) }
            }
            KontaktadresseType.UTLAND -> when {
                kontaktadresse.utenlandskAdresse != null -> tilFormatertAdresse(kontaktadresse.utenlandskAdresse, gjeldendeDato)
                else -> kontaktadresse.utenlandskAdresseIFrittFormat?.let { tilFormatertAdresse(it, gjeldendeDato) }
            }
        }
        return join(coAdresse(kontaktadresse.coAdressenavn), adresse)
    }

    private fun coAdresse(coAdressenavn: String?): String? = coAdressenavn?.let { "c/o $it" }

    //må feltet "postboks" ha med "postboks" i strengen? "postboks ${postboks}" ?
    private fun tilFormatertAdresse(postboksadresse: Postboksadresse, gjeldendeDato: LocalDate): String? =
            join(postboksadresse.postbokseier,
                 postboksadresse.postboks,
                 space(postboksadresse.postnummer, poststed(postboksadresse.postnummer, gjeldendeDato)))

    private fun tilFormatertAdresse(postadresseIFrittFormat: PostadresseIFrittFormat, gjeldendeDato: LocalDate): String? =
            join(postadresseIFrittFormat.adresselinje1,
                 postadresseIFrittFormat.adresselinje2,
                 postadresseIFrittFormat.adresselinje3,
                 space(postadresseIFrittFormat.postnummer, poststed(postadresseIFrittFormat.postnummer, gjeldendeDato)))

    // har ikke med bygningEtasjeLeilighet, postboksNummerNavn
    private fun tilFormatertAdresse(utenlandskAdresse: UtenlandskAdresse, gjeldendeDato: LocalDate): String? {
        return join(utenlandskAdresse.adressenavnNummer,
                    space(utenlandskAdresse.postkode, utenlandskAdresse.bySted),
                    utenlandskAdresse.regionDistriktOmraade,
                    land(utenlandskAdresse.landkode, gjeldendeDato))
    }

    private fun tilFormatertAdresse(utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat,
                                    gjeldendeDato: LocalDate): String? {
        return join(utenlandskAdresseIFrittFormat.adresselinje1,
                    utenlandskAdresseIFrittFormat.adresselinje2,
                    utenlandskAdresseIFrittFormat.adresselinje3,
                    space(utenlandskAdresseIFrittFormat.postkode, utenlandskAdresseIFrittFormat.byEllerStedsnavn),
                    land(utenlandskAdresseIFrittFormat.landkode, gjeldendeDato))
    }

    private fun tilFormatertAdresse(vegadresse: Vegadresse, gjeldendeDato: LocalDate): String? {
        return join(space(vegadresse.adressenavn, vegadresse.husnummer, vegadresse.husbokstav),
                    vegadresse.bruksenhetsnummer,
                    space(vegadresse.postnummer, poststed(vegadresse.postnummer, gjeldendeDato)))
    }

    private fun poststed(postnummer: String?, gjeldendeDato: LocalDate): String? {
        if (postnummer == null) return null
        return kodeverkService.hentPoststed(postnummer, gjeldendeDato)
    }

    private fun land(landkode: String?, gjeldendeDato: LocalDate): String? {
        if (landkode == null) return null
        return kodeverkService.hentLand(landkode, gjeldendeDato)
    }

    private fun space(vararg args: String?): String? = join(*args, separator = " ")

    private fun join(vararg args: String?, separator: String = ", "): String? {
        val filterNotNull = args.filterNotNull().filterNot(String::isEmpty)
        return if (filterNotNull.isEmpty()) {
            null
        } else filterNotNull.joinToString(separator)
    }
}
