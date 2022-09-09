package no.nav.familie.ef.sak.ekstern.journalføring

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.journalføring.JournalføringService
import no.nav.familie.ef.sak.journalføring.JournalpostService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.kontrakter.ef.journalføring.AutomatiskJournalføringResponse
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskJournalføringService(
    private val journalføringService: JournalføringService,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val infotrygdService: InfotrygdService,
    private val behandlingService: BehandlingService
) {

    @Transactional
    fun automatiskJournalførTilFørstegangsbehandling(
        journalpostId: String,
        personIdent: String,
        stønadstype: StønadType,
        mappeId: Long?
    ): AutomatiskJournalføringResponse {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        validerKanAutomatiskJournalføre(personIdent, stønadstype, journalpost)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, stønadstype)
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(fagsak.hentAktivIdent())

        return journalføringService.automatiskJournalførFørstegangsbehandling(fagsak, journalpost, journalførendeEnhet, mappeId)
    }

    fun kanOppretteFørstegangsbehandling(ident: String, type: StønadType): Boolean {
        val allePersonIdenter = personService.hentPersonIdenter(ident).identer.map { it.ident }.toSet()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, type)

        return harIngenReelleBehandlinger(fagsak) && harIngenInnslagIInfotrygd(ident, type)
    }

    private fun validerKanAutomatiskJournalføre(personIdent: String, stønadstype: StønadType, journalpost: Journalpost) {
        val allePersonIdenter = personService.hentPersonIdenter(personIdent).identer.map { it.ident }.toSet()
        feilHvisIkke(kanOppretteFørstegangsbehandling(personIdent, stønadstype)) {
            "Kan ikke opprette førstegangsbehandling for $stønadstype da det allerede finnes en behandling i infotrygd eller ny løsning"
        }

        feilHvis(journalpost.bruker == null) {
            "Journalposten mangler bruker. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }

        journalpost.bruker?.let {
            feilHvisIkke(fagsakPersonOgJournalpostBrukerErSammePerson(allePersonIdenter, personIdent, it)) {
                "Ikke samsvar mellom personident på journalposten og personen vi forsøker å opprette behandling for. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
            }
        }

        feilHvis(journalpost.journalstatus != Journalstatus.MOTTATT) {
            "Journalposten har ugyldig journalstatus ${journalpost.journalstatus}. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }
    }

    private fun fagsakPersonOgJournalpostBrukerErSammePerson(
        allePersonIdenter: Set<String>,
        gjeldendePersonIdent: String,
        journalpostBruker: Bruker
    ): Boolean = when (journalpostBruker.type) {
        BrukerIdType.FNR -> allePersonIdenter.contains(journalpostBruker.id)
        BrukerIdType.AKTOERID -> hentAktørIderForPerson(gjeldendePersonIdent).contains(journalpostBruker.id)
        BrukerIdType.ORGNR -> false
    }

    private fun hentAktørIderForPerson(personIdent: String) =
        personService.hentAktørIder(personIdent).identer.map { it.ident }

    private fun harIngenInnslagIInfotrygd(
        ident: String,
        type: StønadType
    ) = !infotrygdService.eksisterer(ident, setOf(type))

    private fun harIngenReelleBehandlinger(fagsak: Fagsak?): Boolean {
        return fagsak?.let {
            behandlingService.hentBehandlinger(fagsak.id).none { it.resultat != BehandlingResultat.HENLAGT }
        } ?: true
    }
}
