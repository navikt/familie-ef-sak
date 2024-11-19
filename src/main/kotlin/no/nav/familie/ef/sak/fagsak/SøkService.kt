package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.dto.FagsakForSøkeresultat
import no.nav.familie.ef.sak.fagsak.dto.PersonPåAdresse
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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.KjønnMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class SøkService(
    private val fagsakPersonService: FagsakPersonService,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val pdlSaksbehandlerClient: PdlSaksbehandlerClient,
    private val adresseMapper: AdresseMapper,
    private val fagsakService: FagsakService,
    private val vurderingService: VurderingService,
    private val personopplysningerService: PersonopplysningerService,
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

    fun søkEtterPersonerMedSammeAdressePåFagsakPerson(fagsakPersonId: UUID): SøkeresultatPerson {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val personopplysninger = personopplysningerService.hentPersonopplysningerFraRegister(aktivIdent)
        val barnAvBruker = personopplysninger.barn.tilBarnAvBruker()
        return søkEtterPersonerMedSammeAdresse(aktivIdent, barnAvBruker)
    }

    fun søkEtterPersonerMedSammeAdressePåBehandling(behandlingId: UUID): SøkeresultatPerson {
        val (grunnlag) = vurderingService.hentGrunnlagOgMetadata(behandlingId)

        val aktivIdent = behandlingService.hentAktivIdent(behandlingId)
        return søkEtterPersonerMedSammeAdresse(aktivIdent, grunnlag.barnAvBruker())
    }

    private fun søkEtterPersonerMedSammeAdresse(
        brukersPersonIdent: String,
        barnAvBruker: List<BarnAvBrukerDto>,
    ): SøkeresultatPerson {
        val allePersonerPåAdresse: List<PersonPåAdresse> = hentAllePersonerPåAdresse(brukersPersonIdent)

        val personerMedMetadata =
            allePersonerPåAdresse.map { personPåAdresse ->
                val barnetAvBruker = barnAvBruker.find { it.personIdent == personPåAdresse.personIdent }
                personPåAdresse.copy(
                    erSøker = if (personPåAdresse.personIdent == brukersPersonIdent) true else null,
                    erBarn = if (barnetAvBruker != null) true else null,
                    fødselsdato = barnetAvBruker?.fødselsdato,
                )
            }

        return SøkeresultatPerson(
            personer =
                personerMedMetadata
                    .sortedWith(
                        compareByDescending<PersonPåAdresse> { it.erSøker }
                            .thenByDescending { it.erBarn }
                            .thenByDescending { it.fødselsdato },
                    ),
        )
    }

    private fun hentAllePersonerPåAdresse(aktivIdent: String): List<PersonPåAdresse> {
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

        val personSøkResultat = pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(søkeKriterier).hits
        return personSøkResultat.map {
            PersonPåAdresse(
                personIdent =
                    it.person.folkeregisteridentifikator
                        .gjeldende()
                        .identifikasjonsnummer,
                visningsadresse =
                    it.person.bostedsadresse
                        .gjeldende()
                        ?.let { adresseMapper.tilAdresse(it).visningsadresse },
                visningsnavn = NavnDto.fraNavn(it.person.navn.gjeldende()).visningsnavn,
            )
        }
    }

    fun søkPersonUtenFagsak(personIdent: String): SøkeresultatUtenFagsak =
        personService.hentPersonKortBolk(listOf(personIdent))[personIdent]?.let {
            SøkeresultatUtenFagsak(
                personIdent = personIdent,
                navn = it.navn.gjeldende().visningsnavn(),
            )
        }
            ?: throw ApiFeil("Finner ingen personer for søket", HttpStatus.BAD_REQUEST)
}

private fun List<BarnDto>.tilBarnAvBruker(): List<BarnAvBrukerDto> = this.map { BarnAvBrukerDto(it.personIdent, it.fødselsdato) }

data class BarnAvBrukerDto(
    val personIdent: String?,
    val fødselsdato: LocalDate? = null,
)

private fun VilkårGrunnlagDto.barnAvBruker(): List<BarnAvBrukerDto> = this.barnMedSamvær.map { BarnAvBrukerDto(it.registergrunnlag.fødselsnummer, it.registergrunnlag.fødselsdato) }
