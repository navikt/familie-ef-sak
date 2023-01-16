package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunkt
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
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingsjournalpostRepository: BehandlingsjournalpostRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
    private val featureToggleService: FeatureToggleService
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentAktivIdent(behandlingId: UUID): String = behandlingRepository.finnAktivIdent(behandlingId)

    fun hentEksterneIder(behandlingIder: Set<UUID>) = behandlingIder.takeIf { it.isNotEmpty() }
        ?.let { behandlingRepository.finnEksterneIder(it) } ?: emptySet()

    fun finnSisteIverksatteBehandling(fagsakId: UUID) =
        behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

    fun hentGamleUferdigeBehandlinger(stønadtype: StønadType): List<Behandling> {
        val enMånedSiden = LocalDateTime.now().minusMonths(1)

        return behandlingRepository.hentUferdigeBehandlingerFørDato(stønadtype, enMånedSiden)
    }

    fun finnesÅpenBehandling(fagsakId: UUID) =
        behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsakId, FERDIGSTILT)

    fun finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId: UUID): Behandling? =
        behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
            ?: hentBehandlinger(fagsakId).lastOrNull {
                it.status == FERDIGSTILT && it.resultat != HENLAGT
            }

    fun hentBehandlingsjournalposter(behandlingId: UUID): List<Behandlingsjournalpost> {
        return behandlingsjournalpostRepository.findAllByBehandlingId(behandlingId)
    }

    fun hentBehandlingerForGjenbrukAvVilkår(fagsakPersonId: UUID): List<Behandling> {
        return behandlingRepository.finnBehandlingerForGjenbrukAvVilkår(fagsakPersonId)
    }

    @Transactional
    fun opprettMigrering(fagsakId: UUID): Behandling {
        return opprettBehandling(
            behandlingType = BehandlingType.REVURDERING,
            fagsakId = fagsakId,
            behandlingsårsak = BehandlingÅrsak.MIGRERING,
            erMigrering = true
        )
    }

    @Transactional
    fun opprettBehandling(
        behandlingType: BehandlingType,
        fagsakId: UUID,
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        stegType: StegType = VILKÅR,
        behandlingsårsak: BehandlingÅrsak,
        kravMottatt: LocalDate? = null,
        erMigrering: Boolean = false
    ): Behandling {
        brukerfeilHvis(kravMottatt != null && kravMottatt.isAfter(LocalDate.now())) {
            "Kan ikke sette krav mottattdato frem i tid"
        }
        feilHvis(erMigrering && !featureToggleService.isEnabled(Toggle.MIGRERING)) {
            "Feature toggle for migrering er disabled"
        }
        feilHvis(
            behandlingsårsak == BehandlingÅrsak.G_OMREGNING &&
                !featureToggleService.isEnabled(Toggle.G_BEREGNING)
        ) {
            "Feature toggle for g-omregning er disabled"
        }
        feilHvis(
            behandlingsårsak == BehandlingÅrsak.KORRIGERING_UTEN_BREV &&
                !featureToggleService.isEnabled(Toggle.BEHANDLING_KORRIGERING)
        ) {
            "Feature toggle for korrigering er ikke skrudd på for bruker"
        }
        val tidligereBehandlinger = behandlingRepository.findByFagsakId(fagsakId)
        val forrigeBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)
        validerKanOppretteNyBehandling(behandlingType, tidligereBehandlinger, erMigrering)

        val behandling = behandlingRepository.insert(
            Behandling(
                fagsakId = fagsakId,
                forrigeBehandlingId = forrigeBehandling?.id,
                type = behandlingType,
                steg = stegType,
                status = status,
                resultat = BehandlingResultat.IKKE_SATT,
                årsak = behandlingsårsak,
                kravMottatt = kravMottatt
            )
        )

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingshistorikk = Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = VILKÅR
            )
        )

        return behandling
    }

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentSaksbehandling(behandlingId: UUID): Saksbehandling = behandlingRepository.finnSaksbehandling(behandlingId)

    fun hentSaksbehandling(eksternBehandlingId: Long): Saksbehandling =
        behandlingRepository.finnSaksbehandling(eksternBehandlingId)

    fun hentBehandlingPåEksternId(eksternBehandlingId: Long): Behandling = behandlingRepository.finnMedEksternId(
        eksternBehandlingId
    ) ?: error("Kan ikke finne behandling med eksternId=$eksternBehandlingId")

    fun hentBehandlinger(behandlingIder: Set<UUID>): List<Behandling> =
        behandlingRepository.findAllByIdOrThrow(behandlingIder) { it.id }

    fun oppdaterStatusPåBehandling(behandlingId: UUID, status: BehandlingStatus): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId " +
                "fra ${behandling.status} til $status"
        )
        return behandlingRepository.update(behandling.copy(status = status))
    }

    fun oppdaterForrigeBehandlingId(behandlingId: UUID, forrigeBehandlingId: UUID): Behandling {
        val behandling = hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke endre forrigeBehandlingId når behandlingen er låst"
        }
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandler()} endrer forrigeBehandlingId på behandling $behandlingId " +
                "fra ${behandling.forrigeBehandlingId} til $forrigeBehandlingId"
        )
        return behandlingRepository.update(behandling.copy(forrigeBehandlingId = forrigeBehandlingId))
    }

    fun oppdaterStegPåBehandling(behandlingId: UUID, steg: StegType): Behandling {
        val behandling = hentBehandling(behandlingId)
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandler()} endrer steg på behandling $behandlingId " +
                "fra ${behandling.steg} til $steg"
        )
        return behandlingRepository.update(behandling.copy(steg = steg))
    }

    fun oppdaterKravMottatt(behandlingId: UUID, kravMottatt: LocalDate?): Behandling {
        return behandlingRepository.update(hentBehandling(behandlingId).copy(kravMottatt = kravMottatt))
    }

    fun finnesBehandlingForFagsak(fagsakId: UUID) =
        behandlingRepository.existsByFagsakId(fagsakId)

    fun hentBehandlinger(fagsakId: UUID): List<Behandling> {
        return behandlingRepository.findByFagsakId(fagsakId).sortertEtterVedtakstidspunkt()
    }

    fun leggTilBehandlingsjournalpost(journalpostId: String, journalposttype: Journalposttype, behandlingId: UUID) {
        behandlingsjournalpostRepository.insert(
            Behandlingsjournalpost(
                behandlingId = behandlingId,
                journalpostId = journalpostId,
                sporbar = Sporbar(),
                journalpostType = journalposttype
            )
        )
    }

    @Transactional
    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto): Behandling {
        val behandling = hentBehandling(behandlingId)
        validerAtBehandlingenKanHenlegges(behandling)
        val henlagtBehandling = behandling.copy(
            henlagtÅrsak = henlagt.årsak,
            resultat = HENLAGT,
            steg = BEHANDLING_FERDIGSTILT,
            status = FERDIGSTILT,
            vedtakstidspunkt = SporbarUtils.now()
        )
        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = henlagtBehandling.id,
            stegtype = henlagtBehandling.steg,
            utfall = StegUtfall.HENLAGT,
            metadata = henlagt
        )
        opprettStatistikkTask(henlagtBehandling)
        return behandlingRepository.update(henlagtBehandling)
    }

    private fun opprettStatistikkTask(behandling: Behandling) {
        taskService.save(
            BehandlingsstatistikkTask.opprettHenlagtTask(
                behandlingId = behandling.id,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(true)
            )
        )
    }

    private fun validerAtBehandlingenKanHenlegges(behandling: Behandling) {
        if (!behandling.kanHenlegges()) {
            throw ApiFeil(
                "Kan ikke henlegge en behandling med status ${behandling.status} for ${behandling.type}",
                HttpStatus.BAD_REQUEST
            )
        }
    }

    /**
     * Setter endelig resultat på behandling, setter vedtakstidspunkt på behandling
     */
    fun oppdaterResultatPåBehandling(behandlingId: UUID, behandlingResultat: BehandlingResultat): Behandling {
        val behandling = hentBehandling(behandlingId)
        feilHvis(behandlingResultat == BehandlingResultat.IKKE_SATT) {
            "Må sette et endelig resultat og ikke $behandlingResultat"
        }
        feilHvis(behandling.resultat != BehandlingResultat.IKKE_SATT) {
            "Kan ikke endre resultat på behandling når resultat=${behandling.resultat}"
        }
        return behandlingRepository.update(
            behandling.copy(
                resultat = behandlingResultat,
                vedtakstidspunkt = SporbarUtils.now()
            )
        )
    }


}
