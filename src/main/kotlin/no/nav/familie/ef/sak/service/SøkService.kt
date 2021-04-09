package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.BostedsadresseDto
import no.nav.familie.ef.sak.api.dto.FagsakForSøkeresultat
import no.nav.familie.ef.sak.api.dto.NavnDto
import no.nav.familie.ef.sak.api.dto.Søkeresultat
import no.nav.familie.ef.sak.integration.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.integration.dto.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.mapper.KjønnMapper
import no.nav.familie.ef.sak.repository.FagsakRepository
import org.springframework.stereotype.Service

@Service
class SøkService(private val fagsakRepository: FagsakRepository,
                 private val personService: PersonService,
                 private val pdlSaksbehandlerClient: PdlSaksbehandlerClient) {

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

    fun søkPerson(bostedsadresse: BostedsadresseDto): PersonSøkResultat {
        return pdlSaksbehandlerClient.sokPersoner(bostedsadresse = bostedsadresse)
    }

}