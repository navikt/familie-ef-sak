package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.Behandlingsjournalpost
import no.nav.familie.ef.sak.domene.Sporbar
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.behandling.OpprettBehandlingUtil.sistIverksatteBehandling
import no.nav.familie.ef.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad as SøknadOvergangsstønadKontrakt

@Service
class BehandlingService(private val behandlingsjournalpostRepository: BehandlingsjournalpostRepository,
                        private val behandlingRepository: BehandlingRepository,
                        private val behandlingshistorikkService: BehandlingshistorikkService,
                        private val søknadService: SøknadService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    fun hentAktivIdent(behandlingId: UUID): String = behandlingRepository.finnAktivIdent(behandlingId)

    fun hentEksterneIder(behandlingIder: Set<UUID>) = behandlingRepository.finnEksterneIder(behandlingIder)

    fun finnSisteIverksatteBehandlinger(stønadstype: Stønadstype) =
            behandlingRepository.finnSisteIverksatteBehandlingerSomIkkeErTekniskOpphør(stønadstype)

    @Transactional
    fun opprettBehandling(behandlingType: BehandlingType,
                          fagsakId: UUID,
                          søknad: SøknadOvergangsstønadKontrakt,
                          journalpost: Journalpost): Behandling {
        val behandling = opprettBehandling(behandlingType, fagsakId)
        behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandling.id,
                                                                       journalpost.journalpostId,
                                                                       journalpost.journalposttype))
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandling.id, fagsakId, journalpost.journalpostId)
        return behandling
    }

    fun hentBehandlingsjournalposter(behandlingId: UUID): List<Behandlingsjournalpost> {
        return behandlingsjournalpostRepository.findAllByBehandlingId(behandlingId)
    }

    fun opprettBehandling(behandlingType: BehandlingType,
                          fagsakId: UUID,
                          status: BehandlingStatus = BehandlingStatus.OPPRETTET,
                          stegType: StegType = StegType.VILKÅR): Behandling {
        val tidligereBehandlinger = behandlingRepository.findByFagsakId(fagsakId)
        validerKanOppretteNyBehandling(behandlingType, tidligereBehandlinger)
        val forrigeBehandlingId = sistIverksatteBehandling(tidligereBehandlinger)?.id

        return behandlingRepository.insert(Behandling(fagsakId = fagsakId,
                                                      forrigeBehandlingId = forrigeBehandlingId,
                                                      type = behandlingType,
                                                      steg = stegType,
                                                      status = status,
                                                      resultat = BehandlingResultat.IKKE_SATT))
    }

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun oppdaterStatusPåBehandling(behandlingId: UUID, status: BehandlingStatus): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info("${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId " +
                          "fra ${behandling.status} til $status")

        behandling.status = status
        return behandlingRepository.update(behandling)
    }

    fun oppdaterStegPåBehandling(behandlingId: UUID, steg: StegType): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info("${SikkerhetContext.hentSaksbehandler()} endrer steg på behandling $behandlingId " +
                          "fra ${behandling.steg} til $steg")

        behandling.steg = steg
        return behandlingRepository.update(behandling)
    }


    fun hentBehandlinger(fagsakId: UUID): List<Behandling> {
        return behandlingRepository.findByFagsakId(fagsakId).sortedBy { it.sporbar.opprettetTid }
    }

    fun leggTilBehandlingsjournalpost(journalpostId: String, journalposttype: Journalposttype, behandlingId: UUID) {
        behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandlingId = behandlingId,
                                                                       journalpostId = journalpostId,
                                                                       sporbar = Sporbar(),
                                                                       journalpostType = journalposttype))
    }

    fun annullerBehandling(behandlingId: UUID): Behandling {
        val behandling = hentBehandling(behandlingId)
        validerAtBehandlingenKanAnnulleres(behandling)
        behandling.status = BehandlingStatus.FERDIGSTILT
        behandling.resultat = BehandlingResultat.ANNULLERT
        behandling.steg = StegType.BEHANDLING_FERDIGSTILT
        behandlingshistorikkService.opprettHistorikkInnslag(behandling)
        return behandlingRepository.update(behandling)
    }

    private fun validerAtBehandlingenKanAnnulleres(behandling: Behandling) {
        if (!behandling.kanAnnulleres()) {
            throw Feil(
                    message = "Kan ikke annullere en behandling med status ${behandling.status} for ${behandling.type}",
                    frontendFeilmelding = "Kan ikke annullere en behandling med status ${behandling.status} for ${behandling.type}",
                    httpStatus = HttpStatus.BAD_REQUEST,
                    throwable = null
            )
        }
    }

    fun oppdaterResultatPåBehandling(behandlingId: UUID, behandlingResultat: BehandlingResultat) {
        val behandling = hentBehandling(behandlingId)
        behandling.resultat = behandlingResultat
        behandlingRepository.update(behandling)
    }

}
