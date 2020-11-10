package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.ef.sak.service.PersonopplysningerService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate


@RestController
@RequestMapping(path = ["/api/personopplysninger/"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonopplysningerController(private val personopplysningerService: PersonopplysningerService, private val tilgangService: TilgangService) {

    @PostMapping
    fun personopplysninger(@RequestBody personIdent: PersonIdentDto): Ressurs<PersonopplysningerDto> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent.personIdent)
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(personIdent.personIdent))
    }

    @PostMapping("dummy")
    fun personopplysninger(): Ressurs<PersonopplysningerDto>{
        return Ressurs.Companion.success(
                PersonopplysningerDto(personIdent = "12345678910",
                                      navn = NavnDto.fraNavn(Navn("Olav", "mellomnavn", "Olavsen", Metadata(
                                              listOf()))),
                                      kjønn = Kjønn.MANN,
                                      adressebeskyttelse = Adressebeskyttelse.UGRADERT,
                                      folkeregisterpersonstatus = Folkeregisterpersonstatus.BOSATT,
                                      dødsdato = null,
                                      telefonnummer = null,
                                      statsborgerskap = listOf(StatsborgerskapDto(land ="Danmark", gyldigFraOgMedDato = LocalDate.MAX, gyldigTilOgMedDato = null)),
                                      sivilstand = listOf(SivilstandDto(Sivilstandstype.SEPARERT, "20.20.2015", relatertVedSivilstand = "99999", navn = "Annen Forelder")),
                                      adresse = listOf(AdresseDto("Moldegata 15", AdresseType.BOSTEDADRESSE, gyldigFraOgMed = LocalDate.EPOCH, gyldigTilOgMed = null),
                                                       AdresseDto("Holtevegen 355", AdresseType.BOSTEDADRESSE, gyldigFraOgMed = LocalDate.EPOCH, gyldigTilOgMed = null)),
                                      fullmakt = listOf(),
                                      egenAnsatt = false,
                                      navEnhet = "abcd-enhet",
                                      barn = listOf(BarnDto("654321",
                                                            "Mari Olavsdottir",
                                                            AnnenForelderDTO("99999", "Annen Forelder Navn"),
                                                            listOf(AdresseDto("Moldegata 15",
                                                                              AdresseType.BOSTEDADRESSE,
                                                                              gyldigFraOgMed = LocalDate.EPOCH,
                                                                              gyldigTilOgMed = null)),
                                      true)),
                ))
    }

}
