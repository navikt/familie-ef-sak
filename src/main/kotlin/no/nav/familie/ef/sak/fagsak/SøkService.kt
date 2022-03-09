package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.dto.*
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlPersonSøkHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.KjønnMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.*

@Service
class SøkService(
        private val fagsakPersonService: FagsakPersonService,
        private val behandlingService: BehandlingService,
        private val personService: PersonService,
        private val pdlSaksbehandlerClient: PdlSaksbehandlerClient,
        private val adresseMapper: AdresseMapper,
        private val fagsakService: FagsakService
) {

    fun søkPerson(personIdentFraRequest: String): Søkeresultat {
        val personIdenter = personService.hentPersonIdenter(personIdentFraRequest)
        val gjeldendePersonIdent = personIdenter.gjeldende().ident
        if (personIdenter.identer.isEmpty()) {
            throw ApiFeil("Finner ingen personer for søket", HttpStatus.BAD_REQUEST)
        }
        val fagsaker =
                fagsakService.finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(personIdenter.identer(), gjeldendePersonIdent)
        val fagsakPerson = fagsakPersonService.finnPerson(personIdenter.identer())

        val person = personService.hentSøker(gjeldendePersonIdent)

        return Søkeresultat(personIdent = gjeldendePersonIdent,
                            kjønn = KjønnMapper.tilKjønn(person.kjønn.first().kjønn),
                            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
                            fagsakPersonId = fagsakPerson?.id,
                            fagsaker = fagsaker.map {
                                val behandlinger: List<Behandling> = behandlingService.hentBehandlinger(it.id)

                                val erLøpende: Boolean = fagsakService.erLøpende(behandlinger)

                                FagsakForSøkeresultat(fagsakId = it.id,
                                                      stønadstype = it.stønadstype,
                                                      erLøpende = erLøpende,
                                                      erMigrert = it.migrert)
                            }
        )
    }


    // Denne trenger ikke en tilgangskontroll då den ikke returnerer noe fra behandlingen.
    // Pdl gjører tilgangskontroll for søkPersoner
    // Midlertidlig løsning med å hente søker fra PDL
    // dette kan endres til å hente bosstedsadresse fra databasen når PDL-data blir lagret i databasen
    fun søkEtterPersonerMedSammeAdressePåFagsak(fagsakId: UUID): SøkeresultatPerson {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        return søkEtterPersonerMedSammeAdresse(aktivIdent)
    }

    fun søkEtterPersonerMedSammeAdressePåFagsakPerson(fagsakPersonId: UUID): SøkeresultatPerson {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return søkEtterPersonerMedSammeAdresse(aktivIdent)
    }

    fun søkEtterPersonerMedSammeAdressePåBehandling(behandlingId: UUID): SøkeresultatPerson {
        val aktivIdent = behandlingService.hentAktivIdent(behandlingId)
        return søkEtterPersonerMedSammeAdresse(aktivIdent)
    }

    private fun søkEtterPersonerMedSammeAdresse(aktivIdent: String): SøkeresultatPerson {
        val søker = personService.hentSøker(aktivIdent)
        val aktuelleBostedsadresser = søker.bostedsadresse.filterNot { it.metadata.historisk }
        val bostedsadresse = aktuelleBostedsadresser.singleOrNull()
                             ?: throw Feil("Finner 0 eller fler enn 1 bostedsadresse")

        val søkeKriterier = PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(bostedsadresse)
        if (søkeKriterier.isEmpty()) {
            throw Feil(
                    message = "Får ikke laget søkekriterer for bostedsadresse=$bostedsadresse",
                    frontendFeilmelding = "Klarer ikke av å lage søkekriterer for bostedsadressen til person"
            )
        }

        val personSøkResultat = pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(søkeKriterier)

        return SøkeresultatPerson(
                hits = personSøkResultat.hits.map { tilPersonFraSøk(it.person) },
                totalHits = personSøkResultat.totalHits,
                pageNumber = personSøkResultat.pageNumber,
                totalPages = personSøkResultat.totalPages
        )
    }

    fun søkPersonUtenFagsak(personIdent: String): SøkeresultatUtenFagsak {
        return personService.hentPdlPersonKort(listOf(personIdent))[personIdent]?.let {
            SøkeresultatUtenFagsak(
                    personIdent = personIdent,
                    navn = it.navn.gjeldende().visningsnavn()
            )
        }
               ?: throw ApiFeil("Finner ingen personer for søket", HttpStatus.BAD_REQUEST)
    }

    private fun tilPersonFraSøk(person: PdlPersonFraSøk): PersonFraSøk {
        return PersonFraSøk(
                personIdent = person.folkeregisteridentifikator.gjeldende().identifikasjonsnummer,
                visningsadresse = person.bostedsadresse.gjeldende()
                        ?.let { adresseMapper.tilAdresse(it).visningsadresse },
                visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn
        )
    }

}
