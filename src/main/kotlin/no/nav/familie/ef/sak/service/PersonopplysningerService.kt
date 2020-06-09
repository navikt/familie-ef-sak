package no.nav.familie.ef.sak.service

import kotlinx.coroutines.*
import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.api.dto.Adressebeskyttelse
import no.nav.familie.ef.sak.api.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.api.dto.Kjønn
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.pdl.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PersonopplysningerService(private val personService: PersonService,
                                private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentPersonopplysninger(ident: String): PersonopplysningerDto {
        return runBlocking {
            val egenAnsattDeferred = async { familieIntegrasjonerClient.egenAnsatt(ident) }
            val søker = withContext(Dispatchers.Default) { personService.hentPdlPerson(ident) }

            val fullmakter = søker.fullmakt.filter { it.motpartsRolle == MotpartsRolle.FULLMEKTIG }

            val identer = fullmakter.map { it.motpartsPersonident } +
                          søker.sivilstand.mapNotNull { it.relatertVedSivilstand }.filterNot { it.endsWith("00000") }
            val identNavn = hentNavn(identer)

            lagPersonopplysninger(søker, ident, fullmakter, egenAnsattDeferred.await(), identNavn)
        }
    }

    private suspend fun hentNavn(identer: List<String>): Map<String, String> = coroutineScope {
        if (identer.isEmpty()) return@coroutineScope emptyMap<String, String>()
        logger.info("Henter navn til {} personer", identer.size)
        identer.map { Pair(it, async { personService.hentPdlPersonKort(it) }) }
                .map { Pair(it.first, it.second.await().navn.gjeldende().visningsnavn()) }
                .toMap()
    }

    private fun lagPersonopplysninger(søker: PdlSøker,
                                      ident: String,
                                      fullmakter: List<Fullmakt>,
                                      egenAnsatt: Boolean,
                                      identNavn: Map<String, String>): PersonopplysningerDto {
        return PersonopplysningerDto(
                adressebeskyttelse = søker.adressebeskyttelse.firstOrNull()
                        ?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
                folkeregisterpersonstatus = søker.folkeregisterpersonstatus.firstOrNull()
                        ?.let { Folkeregisterpersonstatus.fraPdl(it) },
                dødsdato = søker.dødsfall.firstOrNull()?.dødsdato,
                navn = NavnDto.fraNavn(søker.navn.gjeldende()),
                kjønn = søker.kjønn.single().kjønn.let { Kjønn.valueOf(it.name) },
                personIdent = ident,
                telefonnummer = søker.telefonnummer.find { it.prioritet == 1 }
                        ?.let { TelefonnummerDto(it.landskode, it.nummer) },
                statsborgerskap = søker.statsborgerskap.map {
                    StatsborgerskapDto(land = it.land,
                                       gyldigFraOgMed = it.gyldigFraOgMed,
                                       gyldigTilOgMed = it.gyldigTilOgMed)
                },
                sivilstand = søker.sivilstand.map {
                    SivilstandDto(type = Sivilstandstype.valueOf(it.type.name),
                                  gyldigFraOgMed = it.gyldigFraOgMed?.toString() ?: it.bekreftelsesdato,
                                  relatertVedSivilstand = it.relatertVedSivilstand,
                                  navn = identNavn[it.relatertVedSivilstand])
                },
                adresse = adresse(søker),
                fullmakt = fullmakter.map {
                    FullmaktDto(gyldigFraOgMed = it.gyldigFraOgMed,
                                gyldigTilOgMed = it.gyldigTilOgMed,
                                motpartsPersonident = it.motpartsPersonident,
                                navn = identNavn[it.motpartsPersonident])
                },
                egenAnsatt = egenAnsatt
        )
    }

    private fun adresse(søker: PdlSøker): List<AdresseDto> {
        val adresser =
                søker.bostedsadresse.map {
                    AdresseDto(visningsadresse = it.tilFormatertAdresse(),
                               type = AdresseType.BOSTEDADRESSE,
                               gyldigFraOgMed = it.angittFlyttedato,
                               gyldigTilOgMed = it.folkeregistermetadata.opphørstidspunkt?.toLocalDate())
                } +
                søker.kontaktadresse.map {
                    val type = when (it.type) {
                        KontaktadresseType.INNLAND -> AdresseType.KONTAKTADRESSE
                        KontaktadresseType.UTLAND -> AdresseType.KONTAKTADRESSE_UTLAND
                    }
                    AdresseDto(visningsadresse = it.tilFormatertAdresse(),
                               type = type,
                               gyldigFraOgMed = it.gyldigFraOgMed,
                               gyldigTilOgMed = it.gyldigTilOgMed)
                } +
                søker.oppholdsadresse.map {
                    AdresseDto(visningsadresse = it.tilFormatertAdresse(),
                               type = AdresseType.OPPHOLDSADRESSE,
                               gyldigFraOgMed = it.oppholdsadressedato,
                               gyldigTilOgMed = null)
                }
        return adresser.sortedWith(compareByDescending<AdresseDto>
                                   { it.gyldigFraOgMed ?: LocalDate.MAX }
                                           .thenBy(AdresseDto::type))
    }
}