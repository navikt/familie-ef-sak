package no.nav.familie.ef.sak.service

import kotlinx.coroutines.*
import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.pdl.MotpartsRolle
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.mapper.PersonopplysningerMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersonopplysningerService(private val personService: PersonService,
                                private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                                private val personopplysningerMapper: PersonopplysningerMapper) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentPersonopplysninger(ident: String): PersonopplysningerDto {
        return runBlocking {
            val egenAnsattDeferred = async { familieIntegrasjonerClient.egenAnsatt(ident) }
            val søker = withContext(Dispatchers.Default) { personService.hentPdlPerson(ident) }

            val fullmakter = søker.fullmakt.filter { it.motpartsRolle == MotpartsRolle.FULLMEKTIG }

            val identer = fullmakter.map { it.motpartsPersonident } +
                          søker.sivilstand.mapNotNull { it.relatertVedSivilstand }.filterNot { it.endsWith("00000") }
            val identNavn = hentNavn(identer)

            personopplysningerMapper.tilPersonopplysninger(søker, ident, fullmakter, egenAnsattDeferred.await(), identNavn)
        }
    }

    private suspend fun hentNavn(identer: List<String>): Map<String, String> = coroutineScope {
        if (identer.isEmpty()) return@coroutineScope emptyMap<String, String>()
        logger.info("Henter navn til {} personer", identer.size)
        identer.map { Pair(it, async { personService.hentPdlPersonKort(it) }) }
                .map { Pair(it.first, it.second.await().navn.gjeldende().visningsnavn()) }
                .toMap()
    }
}
