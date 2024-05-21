package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.dto.FagsakForSøkeresultat
import no.nav.familie.ef.sak.fagsak.dto.PersonFraSøk
import no.nav.familie.ef.sak.fagsak.dto.Søkeresultat
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatUtenFagsak
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlPersonSøkHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.KjønnMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SøkService(
    private val fagsakPersonService: FagsakPersonService,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val pdlSaksbehandlerClient: PdlSaksbehandlerClient,
    private val adresseMapper: AdresseMapper,
    private val fagsakService: FagsakService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun søkPersonForEksternFagsak(eksternFagsakId: Long): Søkeresultat {
        val fagsak =
            fagsakService.hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId)
                ?: throw ApiFeil("Finner ikke fagsak for eksternFagsakId=$eksternFagsakId", HttpStatus.BAD_REQUEST)
        val fagsakPerson = fagsakPersonService.hentPerson(fagsak.fagsakPersonId)
        val fagsaker =
            fagsakService.finnFagsakerForFagsakPersonId(fagsak.fagsakPersonId).let {
                listOfNotNull(it.overgangsstønad, it.barnetilsyn, it.skolepenger)
            }
        return tilSøkeresultat(fagsakPerson.hentAktivIdent(), fagsakPerson, fagsaker)
    }

    fun søkPerson(personIdenter: PdlIdenter): Søkeresultat {
        brukerfeilHvis(personIdenter.identer.isEmpty()) {
            "Finner ingen personer for valgt personident"
        }
        val gjeldendePersonIdent = personIdenter.gjeldende().ident
        val fagsaker =
            fagsakService.finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(personIdenter.identer(), gjeldendePersonIdent)
        val fagsakPerson = fagsakPersonService.finnPerson(personIdenter.identer())

        return tilSøkeresultat(gjeldendePersonIdent, fagsakPerson, fagsaker)
    }

    private fun tilSøkeresultat(
        gjeldendePersonIdent: String,
        fagsakPerson: FagsakPerson?,
        fagsaker: List<Fagsak>,
    ): Søkeresultat {
        val person = personService.hentSøker(gjeldendePersonIdent)

        return Søkeresultat(
            personIdent = gjeldendePersonIdent,
            kjønn = KjønnMapper.tilKjønn(person.kjønn.first().kjønn),
            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
            fagsakPersonId = fagsakPerson?.id,
            fagsaker =
                fagsaker.map {
                    FagsakForSøkeresultat(
                        fagsakId = it.id,
                        stønadstype = it.stønadstype,
                        erLøpende = fagsakService.erLøpende(it),
                        erMigrert = it.migrert,
                    )
                },
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
        feilHvis(bostedsadresse == null) {
            "Fant ${aktuelleBostedsadresser.size} bostedsadresser, forventet 1"
        }

        brukerfeilHvis(bostedsadresse.ukjentBosted != null) {
            "Personen har ukjent bostedsadresse, kan ikke finne personer på samme adresse"
        }

        val søkeKriterier = PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(bostedsadresse)
        if (søkeKriterier.isEmpty()) {
            secureLogger.error("Får ikke laget søkekriterer for $aktivIdent med bostedsadresse=$bostedsadresse")
            throw Feil(
                message = "Får ikke laget søkekriterer for bostedsadresse",
                frontendFeilmelding = "Klarer ikke av å lage søkekriterer for bostedsadressen til person",
            )
        }

        val personSøkResultat = pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(søkeKriterier)

        return SøkeresultatPerson(
            hits = personSøkResultat.hits.map { tilPersonFraSøk(it.person) },
            totalHits = personSøkResultat.totalHits,
            pageNumber = personSøkResultat.pageNumber,
            totalPages = personSøkResultat.totalPages,
        )
    }

    fun søkPersonUtenFagsak(personIdent: String): SøkeresultatUtenFagsak {
        return personService.hentPersonKortBolk(listOf(personIdent))[personIdent]?.let {
            SøkeresultatUtenFagsak(
                personIdent = personIdent,
                navn = it.navn.gjeldende().visningsnavn(),
            )
        }
            ?: throw ApiFeil("Finner ingen personer for søket", HttpStatus.BAD_REQUEST)
    }

    private fun tilPersonFraSøk(person: PdlPersonFraSøk): PersonFraSøk {
        return PersonFraSøk(
            personIdent = person.folkeregisteridentifikator.gjeldende().identifikasjonsnummer,
            visningsadresse =
                person.bostedsadresse.gjeldende()
                    ?.let { adresseMapper.tilAdresse(it).visningsadresse },
            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
        )
    }
}
