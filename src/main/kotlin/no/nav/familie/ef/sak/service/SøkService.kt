package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.BostedsadresseDto
import no.nav.familie.ef.sak.api.dto.FagsakForSøkeresultat
import no.nav.familie.ef.sak.api.dto.NavnDto
import no.nav.familie.ef.sak.api.dto.PersonFraSøk
import no.nav.familie.ef.sak.api.dto.Søkeresultat
import no.nav.familie.ef.sak.api.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.integration.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.mapper.AdresseMapper
import no.nav.familie.ef.sak.mapper.KjønnMapper
import no.nav.familie.ef.sak.repository.FagsakRepository
import org.springframework.stereotype.Service

@Service
class SøkService(private val fagsakRepository: FagsakRepository,
                 private val personService: PersonService,
                 private val pdlSaksbehandlerClient: PdlSaksbehandlerClient,
                 private val adresseMapper: AdresseMapper) {

    fun søkPerson(personIdent: String): Søkeresultat {
        val fagsaker = fagsakRepository.findBySøkerIdent(personIdent)

        if (fagsaker.isEmpty()) {
            throw Feil(message = "Finner ikke fagsak for søkte personen",
                       frontendFeilmelding = "Finner ikke fagsak for søkte personen")
        }

        val personIdent = fagsaker.first().hentAktivIdent();
        val person = personService.hentSøker(personIdent)

        return Søkeresultat(personIdent = personIdent,
                            kjønn = KjønnMapper.tilKjønn(person),
                            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
                            fagsaker = fagsaker.map { FagsakForSøkeresultat(fagsakId = it.id, stønadstype = it.stønadstype) }
        )
    }

    fun søkPerson(bostedsadresse: BostedsadresseDto): SøkeresultatPerson {
        val personSøkResultat = pdlSaksbehandlerClient.sokPersoner(bostedsadresse = bostedsadresse)

        return SøkeresultatPerson(hits = personSøkResultat.hits.map { mapHits(it.person) },
                                  totalHits = personSøkResultat.totalHits,
                                  pageNumber = personSøkResultat.pageNumber,
                                  totalPages = personSøkResultat.totalPages
        )
    }

    private fun mapHits(person: PdlPersonFraSøk): PersonFraSøk {
        return PersonFraSøk(personIdent = person.folkeregisteridentifikator.gjeldende().identifikasjonsnummer,
                            visningsadresse = person.bostedsadresse.gjeldende()
                                    ?.let { adresseMapper.tilAdresse(it).visningsadresse },
                            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn)
    }

}