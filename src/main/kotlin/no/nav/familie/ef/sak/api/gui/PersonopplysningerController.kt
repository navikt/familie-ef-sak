package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.api.dto.Adressebeskyttelse
import no.nav.familie.ef.sak.api.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.api.dto.Kjønn
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.service.PersonopplysningerService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month


@RestController
@RequestMapping(path = ["/api/personopplysninger/"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonopplysningerController(private val personopplysningerService: PersonopplysningerService,
                                   private val tilgangService: TilgangService) {

    @PostMapping
    fun personopplysninger(@RequestBody personIdent: PersonIdentDto): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent.personIdent)
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(personIdent.personIdent))
    }

    @PostMapping("dummy")
    fun personopplysninger(): Ressurs<PersonopplysningerDto> {
        return Ressurs.Companion.success(
                PersonopplysningerDto(personIdent = "12345678910",
                                      navn = NavnDto.fraNavn(Navn("Olav", "mellomnavn", "Olavsen", Metadata(
                                              listOf()))),
                                      kjønn = Kjønn.MANN,
                                      adressebeskyttelse = Adressebeskyttelse.UGRADERT,
                                      folkeregisterpersonstatus = Folkeregisterpersonstatus.BOSATT,
                                      dødsdato = null,
                                      telefonnummer = TelefonnummerDto("47", "22228888"),
                                      statsborgerskap = listOf(StatsborgerskapDto(land = "Danmark",
                                                                                  gyldigFraOgMedDato = LocalDate.EPOCH,
                                                                                  gyldigTilOgMedDato = LocalDate.of(1998, 3, 20)),
                                                               StatsborgerskapDto(land = "Norge",
                                                                                  gyldigFraOgMedDato = LocalDate.of(1998, 3, 20),
                                                                                  gyldigTilOgMedDato = null)),
                                      sivilstand = listOf(SivilstandDto(Sivilstandstype.SEPARERT,
                                                                        "20.20.2015",
                                                                        relatertVedSivilstand = "66666999999",
                                                                        navn = "Annen Forelder")),
                                      adresse = listOf(AdresseDto("Moldegata 15",
                                                                  AdresseType.BOSTEDADRESSE,
                                                                  gyldigFraOgMed = LocalDate.EPOCH,
                                                                  gyldigTilOgMed = null),
                                                       AdresseDto("Holtevegen 355",
                                                                  AdresseType.KONTAKTADRESSE,
                                                                  gyldigFraOgMed = LocalDate.EPOCH,
                                                                  gyldigTilOgMed = null)),
                                      fullmakt = listOf(),
                                      egenAnsatt = false,
                                      navEnhet = "0806 Skien",
                                      barn = listOf(BarnDto("05101822222",
                                                            "Mari Olavsdottir",
                                                            AnnenForelderDTO("66666999999", "Annen Forelder Navn"),
                                                            listOf(AdresseDto("Moldegata 15",
                                                                              AdresseType.BOSTEDADRESSE,
                                                                              gyldigFraOgMed = LocalDate.EPOCH,
                                                                              gyldigTilOgMed = null)),
                                                            true),
                                                    BarnDto("01030122222",
                                                            "Nils Olavsson",
                                                            AnnenForelderDTO("66666999999", "Annen Forelder Navn"),
                                                            listOf(AdresseDto("Moldegata 15",
                                                                              AdresseType.BOSTEDADRESSE,
                                                                              gyldigFraOgMed = LocalDate.EPOCH,
                                                                              gyldigTilOgMed = null)),
                                                            false)),
                                      innflyttingTilNorge = listOf(InnflyttingTilNorge("Sverige", null, Folkeregistermetadata(
                                              LocalDateTime.of(1993, Month.AUGUST, 10, 10,10), null))),
                                      utflyttingFraNorge = listOf(UtflyttingFraNorge("Narnia", null, Folkeregistermetadata(
                                              LocalDateTime.of(1998, Month.AUGUST, 10, 10,10), null)))
                ))
    }

}
