package no.nav.familie.ef.sak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.domene.SøkerMedBarn
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
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
            val personMedRelasjoner = withContext(Dispatchers.Default) { personService.hentPersonMedRelasjoner(ident) }

            val fullmakter = personMedRelasjoner.søker.fullmakt.filter { it.motpartsRolle == MotpartsRolle.FULLMEKTIG }

            val identer = fullmakter.map { it.motpartsPersonident } +
                          personMedRelasjoner.søker.sivilstand.mapNotNull { it.relatertVedSivilstand }
                                  .filterNot { it.endsWith("00000") }
            val identNavn = hentGjeldeneNavn(identer + andreForelderIdenter(personMedRelasjoner))
            personopplysningerMapper.tilPersonopplysninger(
                    personMedRelasjoner,
                    ident,
                    fullmakter,
                    egenAnsattDeferred.await(),
                    identNavn
            )
        }
    }

    private fun andreForelderIdenter(personMedRelasjoner: SøkerMedBarn): List<String> =
            personMedRelasjoner.barn.values
                    .flatMap { it.forelderBarnRelasjon }
                    .filter { it.relatertPersonsIdent != personMedRelasjoner.søkerIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                    .map { it.relatertPersonsIdent }
                    .distinct()

    fun hentGjeldeneNavn(identer: List<String>): Map<String, String> {
        if (identer.isEmpty()) return emptyMap()
        logger.info("Henter navn til {} personer", identer.size)
        return personService.hentPdlPersonKort(identer).map { it.key to it.value.navn.gjeldende().visningsnavn() }.toMap()
    }
}
