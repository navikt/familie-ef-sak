package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.HENLAGT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.Behandlingsjournalpost
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.VILKÅR
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad as SøknadOvergangsstønadKontrakt

@Service
class BehandlingService(private val behandlingsjournalpostRepository: BehandlingsjournalpostRepository,
                        private val behandlingRepository: BehandlingRepository,
                        private val behandlingshistorikkService: BehandlingshistorikkService,
                        private val taskService: TaskService,
                        private val søknadService: SøknadService,
                        private val featureToggleService: FeatureToggleService) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    fun hentAktivIdent(behandlingId: UUID): String = behandlingRepository.finnAktivIdent(behandlingId)

    fun hentEksterneIder(behandlingIder: Set<UUID>) = behandlingIder.takeIf { it.isNotEmpty() }
            ?.let { behandlingRepository.finnEksterneIder(it) }

    fun finnSisteIverksatteBehandling(fagsakId: UUID) =
            behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

    fun finnGjeldendeIverksatteBehandlinger(stonadstype: Stønadstype) =
            behandlingRepository.finnSisteIverksatteBehandlinger(stonadstype)

    @Transactional
    fun opprettBehandlingForBlankett(behandlingType: BehandlingType,
                                     fagsakId: UUID,
                                     søknad: SøknadOvergangsstønadKontrakt,
                                     journalpost: Journalpost): Behandling {
        val behandling =
                opprettBehandling(behandlingType = behandlingType, fagsakId = fagsakId, behandlingsårsak = BehandlingÅrsak.SØKNAD)
        behandlingsjournalpostRepository.insert(Behandlingsjournalpost(behandling.id,
                                                                       journalpost.journalpostId,
                                                                       journalpost.journalposttype))
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandling.id, fagsakId, journalpost.journalpostId)
        return behandling
    }

    fun hentBehandlingsjournalposter(behandlingId: UUID): List<Behandlingsjournalpost> {
        return behandlingsjournalpostRepository.findAllByBehandlingId(behandlingId)
    }

    @Transactional
    fun opprettMigrering(fagsakId: UUID): Behandling {
        return opprettBehandling(behandlingType = BehandlingType.REVURDERING,
                                 fagsakId = fagsakId,
                                 behandlingsårsak = BehandlingÅrsak.MIGRERING,
                                 erMigrering = true)
    }

    @Transactional
    fun opprettBehandling(behandlingType: BehandlingType,
                          fagsakId: UUID,
                          status: BehandlingStatus = BehandlingStatus.OPPRETTET,
                          stegType: StegType = VILKÅR,
                          behandlingsårsak: BehandlingÅrsak,
                          kravMottatt: LocalDate? = null,
                          erMigrering: Boolean = false): Behandling {
        feilHvis(erMigrering && !featureToggleService.isEnabled("familie.ef.sak.migrering")) {
            "Feature toggle for migrering er disabled"
        }

        val tidligereBehandlinger = behandlingRepository.findByFagsakId(fagsakId)
        val forrigeBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
        validerKanOppretteNyBehandling(behandlingType, tidligereBehandlinger, forrigeBehandling, erMigrering)

        val behandling = behandlingRepository.insert(Behandling(fagsakId = fagsakId,
                                                                forrigeBehandlingId = forrigeBehandling?.id,
                                                                type = behandlingType,
                                                                steg = stegType,
                                                                status = status,
                                                                resultat = BehandlingResultat.IKKE_SATT,
                                                                årsak = behandlingsårsak,
                                                                kravMottatt = kravMottatt))

        behandlingshistorikkService.opprettHistorikkInnslag(
                behandlingshistorikk = Behandlingshistorikk(behandlingId = behandling.id,
                                                            steg = VILKÅR))

        return behandling
    }

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingPåEksternId(eksternBehandlingId: Long): Behandling = behandlingRepository.finnMedEksternId(
            eksternBehandlingId) ?: error("Kan ikke finne behandling med eksternId=$eksternBehandlingId")

    fun hentBehandlinger(behandlingIder: Set<UUID>): List<Behandling> =
            behandlingRepository.findAllByIdOrThrow(behandlingIder) { it.id }

    fun oppdaterStatusPåBehandling(behandlingId: UUID, status: BehandlingStatus): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info("${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId " +
                          "fra ${behandling.status} til $status")
        return behandlingRepository.update(behandling.copy(status = status))
    }

    fun oppdaterStegPåBehandling(behandlingId: UUID, steg: StegType): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info("${SikkerhetContext.hentSaksbehandler()} endrer steg på behandling $behandlingId " +
                          "fra ${behandling.steg} til $steg")
        return behandlingRepository.update(behandling.copy(steg = steg))
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

    @Transactional
    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto): Behandling {
        val behandling = hentBehandling(behandlingId)
        validerAtBehandlingenKanHenlegges(behandling)
        val henlagtBehandling = behandling.copy(henlagtÅrsak = henlagt.årsak,
                                                resultat = HENLAGT,
                                                steg = BEHANDLING_FERDIGSTILT,
                                                status = FERDIGSTILT)
        behandlingshistorikkService.opprettHistorikkInnslag(behandling = henlagtBehandling,
                                                            utfall = StegUtfall.HENLAGT,
                                                            metadata = henlagt)
        return behandlingRepository.update(henlagtBehandling)
    }

    private fun validerAtBehandlingenKanHenlegges(behandling: Behandling) {
        if (!behandling.kanHenlegges()) {
            throw Feil(
                    "Kan ikke henlegge en behandling med status ${behandling.status} for ${behandling.type}",
                    "Kan ikke henlegge en behandling med status ${behandling.status} for ${behandling.type}",
                    HttpStatus.BAD_REQUEST,
                    null
            )
        }
    }

    fun oppdaterResultatPåBehandling(behandlingId: UUID, behandlingResultat: BehandlingResultat): Behandling {
        val behandling = hentBehandling(behandlingId)
        return behandlingRepository.update(behandling.copy(resultat = behandlingResultat))
    }

    @Transactional
    fun settPåVent(behandlingId: UUID) {
        val behandling = hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering(),
                 HttpStatus.BAD_REQUEST) { "Kan ikke sette behandling med status ${behandling.status} på vent" }

        behandlingRepository.update(behandling.copy(status = BehandlingStatus.SATT_PÅ_VENT))
        taskService.save(BehandlingsstatistikkTask.opprettVenterTask(behandlingId))
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        val behandling = hentBehandling(behandlingId)
        feilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT,
                 HttpStatus.BAD_REQUEST) { "Kan ikke ta behandling med status ${behandling.status} av vent" }
        behandlingRepository.update(behandling.copy(status = BehandlingStatus.UTREDES))
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId))
    }
}
