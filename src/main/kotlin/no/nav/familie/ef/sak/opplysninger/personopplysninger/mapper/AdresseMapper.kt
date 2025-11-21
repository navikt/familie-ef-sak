package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.felles.kodeverk.KodeverkService
import no.nav.familie.ef.sak.felles.util.datoEllerIdag
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KontaktadresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Matrikkeladresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PostadresseIFrittFormat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Postboksadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtenlandskAdresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtenlandskAdresseIFrittFormat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AdresseMapper(
    private val kodeverkService: KodeverkService,
) {
    fun tilAdresse(adresse: Oppholdsadresse): AdresseDto {
        val visningsadresse = tilFormatertAdresse(adresse, datoEllerIdag(adresse.gyldigFraOgMed))
        return AdresseDto(
            visningsadresse = visningsadresse,
            type = AdresseType.OPPHOLDSADRESSE,
            gyldigFraOgMed = adresse.gyldigFraOgMed,
            gyldigTilOgMed = adresse.gyldigTilOgMed,
        )
    }

    fun tilAdresse(adresse: Kontaktadresse): AdresseDto {
        val type =
            when (adresse.type) {
                KontaktadresseType.INNLAND -> AdresseType.KONTAKTADRESSE
                KontaktadresseType.UTLAND -> AdresseType.KONTAKTADRESSE_UTLAND
            }
        return AdresseDto(
            visningsadresse = tilFormatertAdresse(adresse, datoEllerIdag(adresse.gyldigFraOgMed)),
            type = type,
            gyldigFraOgMed = adresse.gyldigFraOgMed,
            gyldigTilOgMed = adresse.gyldigTilOgMed,
        )
    }

    fun tilAdresse(adresse: Bostedsadresse): AdresseDto {
        val gjeldendeDato = datoEllerIdag(adresse.gyldigFraOgMed)
        return AdresseDto(
            visningsadresse = tilFormatertAdresse(adresse, gjeldendeDato),
            type = AdresseType.BOSTEDADRESSE,
            gyldigFraOgMed = adresse.gyldigFraOgMed,
            gyldigTilOgMed = adresse.gyldigTilOgMed,
            angittFlyttedato = angittFlyttedato(adresse.angittFlyttedato),
            erGjeldende = !adresse.metadata.historisk,
        )
    }

    private fun angittFlyttedato(localDate: LocalDate?): LocalDate? =
        if (localDate == LocalDate.of(1, 1, 1)) {
            null
        } else {
            localDate
        }

    private fun tilFormatertAdresse(
        bostedsadresse: Bostedsadresse,
        gjeldendeDato: LocalDate,
    ): String? {
        val (_, _, _, coAdressenavn, utenlandskAdresse, vegadresse, ukjentBosted, matrikkeladresse, _) = bostedsadresse
        val coAdresse = coAdresse(coAdressenavn)
        val formattertAdresse: String? =
            when {
                vegadresse != null -> tilFormatertAdresse(vegadresse, gjeldendeDato)
                matrikkeladresse != null -> tilFormatertAdresse(matrikkeladresse, gjeldendeDato)
                utenlandskAdresse != null -> tilFormatertAdresse(utenlandskAdresse, gjeldendeDato)
                ukjentBosted != null -> "Ukjent bosted - ${ukjentBosted.bostedskommune}"
                coAdresse != null -> ""
                else -> "Ingen opplysninger tilgjenglig"
            }
        return join(coAdresse, formattertAdresse)
    }

    private fun tilFormatertAdresse(
        oppholdsadresse: Oppholdsadresse,
        gjeldendeDato: LocalDate,
    ): String? {
        val adresse =
            oppholdsadresse.vegadresse?.let { tilFormatertAdresse(it, gjeldendeDato) }
                ?: oppholdsadresse.utenlandskAdresse?.let { tilFormatertAdresse(it, gjeldendeDato) }
        return join(coAdresse(oppholdsadresse.coAdressenavn), adresse)
    }

    private fun tilFormatertAdresse(
        kontaktadresse: Kontaktadresse,
        gjeldendeDato: LocalDate,
    ): String? {
        val adresse =
            when (kontaktadresse.type) {
                KontaktadresseType.INNLAND -> {
                    when {
                        kontaktadresse.vegadresse != null -> tilFormatertAdresse(kontaktadresse.vegadresse, gjeldendeDato)
                        kontaktadresse.postboksadresse != null -> tilFormatertAdresse(kontaktadresse.postboksadresse, gjeldendeDato)
                        else -> kontaktadresse.postadresseIFrittFormat?.let { tilFormatertAdresse(it, gjeldendeDato) }
                    }
                }

                KontaktadresseType.UTLAND -> {
                    when {
                        kontaktadresse.utenlandskAdresse != null -> tilFormatertAdresse(kontaktadresse.utenlandskAdresse, gjeldendeDato)
                        else -> kontaktadresse.utenlandskAdresseIFrittFormat?.let { tilFormatertAdresse(it, gjeldendeDato) }
                    }
                }
            }
        return join(coAdresse(kontaktadresse.coAdressenavn), adresse)
    }

    private fun coAdresse(coAdressenavn: String?): String? {
        if (coAdressenavn.isNullOrBlank()) {
            return null
        } else if (harPrefiks(coAdressenavn)) {
            return coAdressenavn
        }
        return "c/o $coAdressenavn"
    }

    private fun harPrefiks(coAdressenavn: String): Boolean =
        coAdressenavn.startsWith("c/o", true) ||
            coAdressenavn.startsWith("V/", true) ||
            coAdressenavn.startsWith("DBO v/", true) ||
            coAdressenavn.startsWith("v/DBO", true)

    // må feltet "postboks" ha med "postboks" i strengen? "postboks ${postboks}" ?
    private fun tilFormatertAdresse(
        postboksadresse: Postboksadresse,
        gjeldendeDato: LocalDate,
    ): String? =
        join(
            postboksadresse.postbokseier,
            postboksadresse.postboks,
            space(postboksadresse.postnummer, poststed(postboksadresse.postnummer, gjeldendeDato)),
        )

    private fun tilFormatertAdresse(
        matrikkeladresse: Matrikkeladresse,
        gjeldendeDato: LocalDate,
    ): String? =
        join(
            matrikkeladresse.tilleggsnavn,
            matrikkeladresse.bruksenhetsnummer,
            space(matrikkeladresse.postnummer, poststed(matrikkeladresse.postnummer, gjeldendeDato)),
        )

    private fun tilFormatertAdresse(
        postadresseIFrittFormat: PostadresseIFrittFormat,
        gjeldendeDato: LocalDate,
    ): String? =
        join(
            postadresseIFrittFormat.adresselinje1,
            postadresseIFrittFormat.adresselinje2,
            postadresseIFrittFormat.adresselinje3,
            space(postadresseIFrittFormat.postnummer, poststed(postadresseIFrittFormat.postnummer, gjeldendeDato)),
        )

    // har ikke med bygningEtasjeLeilighet, postboksNummerNavn
    private fun tilFormatertAdresse(
        utenlandskAdresse: UtenlandskAdresse,
        gjeldendeDato: LocalDate,
    ): String? =
        join(
            utenlandskAdresse.adressenavnNummer,
            space(utenlandskAdresse.postkode, utenlandskAdresse.bySted),
            utenlandskAdresse.regionDistriktOmraade,
            land(utenlandskAdresse.landkode, gjeldendeDato),
        )

    private fun tilFormatertAdresse(
        utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat,
        gjeldendeDato: LocalDate,
    ): String? =
        join(
            utenlandskAdresseIFrittFormat.adresselinje1,
            utenlandskAdresseIFrittFormat.adresselinje2,
            utenlandskAdresseIFrittFormat.adresselinje3,
            space(utenlandskAdresseIFrittFormat.postkode, utenlandskAdresseIFrittFormat.byEllerStedsnavn),
            land(utenlandskAdresseIFrittFormat.landkode, gjeldendeDato),
        )

    private fun tilFormatertAdresse(
        vegadresse: Vegadresse,
        gjeldendeDato: LocalDate,
    ): String? =
        join(
            space(vegadresse.adressenavn, vegadresse.husnummer, vegadresse.husbokstav),
            vegadresse.tilleggsnavn,
            vegadresse.bruksenhetsnummer,
            space(vegadresse.postnummer, poststed(vegadresse.postnummer, gjeldendeDato)),
        )

    private fun poststed(
        postnummer: String?,
        gjeldendeDato: LocalDate,
    ): String? {
        if (postnummer == null) return null
        return kodeverkService.hentPoststed(postnummer, gjeldendeDato)
    }

    private fun land(
        landkode: String?,
        gjeldendeDato: LocalDate,
    ): String? {
        if (landkode == null) return null
        return kodeverkService.hentLand(landkode, gjeldendeDato)
    }

    private fun space(vararg args: String?): String? = join(*args, separator = " ")

    private fun join(
        vararg args: String?,
        separator: String = ", ",
    ): String? {
        val filterNotNull = args.filterNotNull().filterNot(String::isEmpty)
        return if (filterNotNull.isEmpty()) {
            null
        } else {
            filterNotNull.joinToString(separator)
        }
    }
}
