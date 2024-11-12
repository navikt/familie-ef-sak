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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.KjønnMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
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
        return søkEtterPersonerMedSammeAdresseMedPersonopplysninger(aktivIdent, personopplysninger)
    }

    fun søkEtterPersonerMedSammeAdressePåBehandling(behandlingId: UUID): SøkeresultatPerson {
        val (grunnlag) = vurderingService.hentGrunnlagOgMetadata(behandlingId)

        val aktivIdent = behandlingService.hentAktivIdent(behandlingId)
        return søkEtterPersonerMedSammeAdresseMedGrunnlag(aktivIdent, grunnlag)
    }

    private fun søkEtterPersonerMedSammeAdresseMedPersonopplysninger(
        aktivIdent: String,
        personopplysninger: PersonopplysningerDto,
    ): SøkeresultatPerson {
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

        return SøkeresultatPerson(
            personer =
                personSøkResultat
                    .map { tilPersonFraSøkMedPersonsonopplysninger(it.person, personopplysninger) }
                    .sortedWith(
                        compareByDescending<PersonFraSøk> { it.erSøker }
                            .thenByDescending { it.erBarn }
                            .thenByDescending { it.fødselsdato },
                    ),
        )
    }

    private fun søkEtterPersonerMedSammeAdresseMedGrunnlag(
        aktivIdent: String,
        grunnlag: VilkårGrunnlagDto,
    ): SøkeresultatPerson {
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

        return SøkeresultatPerson(
            personer =
                personSøkResultat
                    .map { tilPersonFraSøkMedGrunnlag(it.person, grunnlag) }
                    .sortedWith(
                        compareByDescending<PersonFraSøk> { it.erSøker }
                            .thenByDescending { it.erBarn }
                            .thenByDescending { it.fødselsdato },
                    ),
        )
    }

    fun søkPersonUtenFagsak(personIdent: String): SøkeresultatUtenFagsak =
        personService.hentPersonKortBolk(listOf(personIdent))[personIdent]?.let {
            SøkeresultatUtenFagsak(
                personIdent = personIdent,
                navn = it.navn.gjeldende().visningsnavn(),
            )
        }
            ?: throw ApiFeil("Finner ingen personer for søket", HttpStatus.BAD_REQUEST)

    private fun tilPersonFraSøkMedGrunnlag(
        person: PdlPersonFraSøk,
        grunnlag: VilkårGrunnlagDto,
    ): PersonFraSøk {
        val personIdent = person.folkeregisteridentifikator.gjeldende().identifikasjonsnummer

        val erSøker = if (grunnlag.personalia.personIdent == personIdent) true else null

        val erBarn = if (grunnlag.barnMedSamvær.any { it.registergrunnlag.fødselsnummer == personIdent }) true else null

        val barnFødselsdato =
            grunnlag
                .barnMedSamvær
                .find { it.registergrunnlag.fødselsnummer == personIdent }
                ?.registergrunnlag
                ?.fødselsdato

        return PersonFraSøk(
            personIdent = personIdent,
            visningsadresse = person.bostedsadresse.gjeldende()?.let { adresseMapper.tilAdresse(it).visningsadresse },
            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
            fødselsdato = barnFødselsdato,
            erSøker = erSøker,
            erBarn = erBarn,
        )
    }

    private fun tilPersonFraSøkMedPersonsonopplysninger(
        person: PdlPersonFraSøk,
        personopplysninger: PersonopplysningerDto,
    ): PersonFraSøk {
        val personIdent = person.folkeregisteridentifikator.gjeldende().identifikasjonsnummer

        val erSøker = if (personopplysninger.personIdent == personIdent) true else null

        val erBarn = if (personopplysninger.barn.any { it.personIdent == personIdent }) true else null

        val barnFødselsdato = personopplysninger.barn.find { it.personIdent == personIdent }?.fødselsdato

        return PersonFraSøk(
            personIdent = personIdent,
            visningsadresse = person.bostedsadresse.gjeldende()?.let { adresseMapper.tilAdresse(it).visningsadresse },
            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
            fødselsdato = barnFødselsdato,
            erSøker = erSøker,
            erBarn = erBarn,
        )
    }
}
