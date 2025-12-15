package no.nav.familie.ef.sak.ekstern.journalføring

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.journalføring.JournalføringHelper.utledNesteBehandlingstype
import no.nav.familie.ef.sak.journalføring.JournalføringService
import no.nav.familie.ef.sak.journalføring.JournalpostService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.ef.journalføring.AutomatiskJournalføringResponse
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
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
    private val behandlingService: BehandlingService,
) {
    private val logger = Logg.getLogger(this::class)

    @Transactional
    fun automatiskJournalførTilBehandling(
        journalpostId: String,
        personIdent: String,
        stønadstype: StønadType,
        mappeId: Long?,
        prioritet: OppgavePrioritet,
    ): AutomatiskJournalføringResponse {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, stønadstype)
        val nesteBehandlingstype = utledNesteBehandlingstype(behandlingService.hentBehandlinger(fagsak.id))
        val journalførendeEnhet =
            arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(fagsak.hentAktivIdent())

        validerKanAutomatiskJournalføre(personIdent, stønadstype, journalpost)

        return journalføringService.automatiskJournalfør(
            fagsak,
            journalpost,
            journalførendeEnhet,
            mappeId,
            nesteBehandlingstype,
            prioritet,
        )
    }

    fun kanOppretteBehandling(
        ident: String,
        stønadstype: StønadType,
    ): Boolean {
        val allePersonIdenter = personService.hentPersonIdenter(ident).identer()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, stønadstype)
        val behandlinger = fagsak?.let { behandlingService.hentBehandlinger(fagsak.id) } ?: emptyList()
        val behandlingstype = utledNesteBehandlingstype(behandlinger)

        return when (behandlingstype) {
            FØRSTEGANGSBEHANDLING -> return when (stønadstype) {
                StønadType.OVERGANGSSTØNAD -> harIngenInnslagIInfotrygd(ident, stønadstype)
                else -> true
            }

            REVURDERING -> kanAutomatiskJournalføreRevurdering(behandlinger, fagsak)
        }
    }

    private fun kanAutomatiskJournalføreRevurdering(
        behandlinger: List<Behandling>,
        fagsak: Fagsak?,
    ): Boolean =
        if (!harÅpenBehandling(behandlinger)) {
            logger.info("Kan automatisk journalføre for fagsak: ${fagsak?.id}")
            true
        } else {
            logger.info("Kan ikke automatisk journalføre for fagsak: ${fagsak?.id}")
            false
        }

    private fun validerKanAutomatiskJournalføre(
        personIdent: String,
        stønadstype: StønadType,
        journalpost: Journalpost,
    ) {
        val allePersonIdenter = personService.hentPersonIdenter(personIdent).identer()

        feilHvisIkke(kanOppretteBehandling(personIdent, stønadstype)) {
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
        journalpostBruker: Bruker,
    ): Boolean =
        when (journalpostBruker.type) {
            BrukerIdType.FNR -> allePersonIdenter.contains(journalpostBruker.id)
            BrukerIdType.AKTOERID -> hentAktørIderForPerson(gjeldendePersonIdent).contains(journalpostBruker.id)
            BrukerIdType.ORGNR -> false
        }

    private fun hentAktørIderForPerson(personIdent: String) = personService.hentAktørIder(personIdent).identer()

    private fun harIngenInnslagIInfotrygd(
        ident: String,
        type: StønadType,
    ) = !infotrygdService.eksisterer(ident, setOf(type))

    private fun harÅpenBehandling(behandlinger: List<Behandling>): Boolean = behandlinger.any { !it.erAvsluttet() }
}
