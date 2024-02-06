package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
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
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping
    fun sjekkIdenter(@RequestBody identer: Set<String>): Int {
        tilgangService.validerHarForvalterrolle()
        val count = identer.map { aktørId ->
            val personIdenter = personService.hentPersonIdenter(aktørId)
            val fagsaker = fagsakService.finnFagsaker(personIdenter.identer())
            if (fagsaker.isNotEmpty()) {
                fagsaker.forEach {
                    val behandlinger = fagsakService.fagsakTilDto(it).behandlinger
                    secureLogger.info("Fagsak=${it.id} har ${behandlinger.size} behandlinger")
                }
            }
            fagsaker.isNotEmpty()
        }.count { it }
        secureLogger.info("$count personer funnet")
        return count
    }
}
