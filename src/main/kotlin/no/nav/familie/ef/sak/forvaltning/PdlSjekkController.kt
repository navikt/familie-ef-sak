package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/pdl-sjekk"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class PdlSjekkController(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val tilgangService: TilgangService,
    private val personopplysningerService: PersonopplysningerService,
) {
    private val logger = Logg.getLogger(this::class)

    @PostMapping
    fun sjekkIdenter(
        @RequestBody identer: Set<String>,
    ): Int {
        tilgangService.validerHarForvalterrolle()
        val count =
            identer
                .map { aktørId ->
                    val personIdenter = personService.hentPersonIdenter(aktørId)
                    val fagsaker = fagsakService.finnFagsaker(personIdenter.identer())
                    if (fagsaker.isNotEmpty()) {
                        fagsaker.forEach {
                            val behandlinger = fagsakService.fagsakTilDto(it).behandlinger
                            logger.info("Fagsak=${it.id} har ${behandlinger.size} behandlinger")
                        }
                    }
                    fagsaker.isNotEmpty()
                }.count { it }
        logger.info("$count personer funnet")
        return count
    }

    @PostMapping("/annenforelder")
    fun sjekkAnnenForelder(
        @RequestBody ident: String,
    ): Int {
        tilgangService.validerHarForvalterrolle()
        val søker = personService.hentSøker(ident)
        val strengesteAdressebeskyttelseForPersonMedRelasjoner = personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(ident)

        if (strengesteAdressebeskyttelseForPersonMedRelasjoner != ADRESSEBESKYTTELSEGRADERING.UGRADERT) {
            return 0
        }

        val barn = hentPdlBarn(søker)
        val andreForeldre =
            hentPdlBarneForeldre(
                barn = barn,
                personIdent = ident,
            )

        logger.info("Søker har ${barn.size} barn og ${andreForeldre.size} andre foreldre")

        andreForeldre.forEach { (id, forelder) ->
            logger.info("Annen forelder: $id - ${forelder.navn}  ")
            forelder.folkeregisteridentifikator.forEach { pid ->
                logger.info("Annen forelder-ident status: ${pid.status}, historisk = ${pid.metadata.historisk}")
            }
        }

        return andreForeldre.size
    }

    private fun hentPdlBarneForeldre(
        barn: Map<String, PdlPersonForelderBarn>,
        personIdent: String,
    ): Map<String, PdlAnnenForelder> =
        barn
            .flatMap { it.value.forelderBarnRelasjon }
            .filter { it.relatertPersonsIdent != personIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .distinct()
            .let { personService.hentAndreForeldre(it) }

    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlPersonForelderBarn> =
        pdlSøker.forelderBarnRelasjon
            .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
            .mapNotNull { it.relatertPersonsIdent }
            .let { personService.hentPersonForelderBarnRelasjon(it) }
}
