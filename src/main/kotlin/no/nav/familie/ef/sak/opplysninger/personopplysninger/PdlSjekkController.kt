package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/pdl-sjekk"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
@Validated
class PdlSjekkController(
    private val personService: PersonService,
    private val fagsakService: FagsakService
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping
    fun sjekkIdenter(@RequestBody identer: Set<String>): Int {
        val count = identer.map { aktørId ->
            val identer = personService.hentPersonIdenter(aktørId, true)
            val fagsaker = fagsakService.finnFagsaker(identer.identer())
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
