package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BESLUTTE_VEDTAK
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegService(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val rolleConfig: RolleConfig,
    private val behandlingshistorikkService: BehandlingshistorikkService,
) {
    private val logger = Logg.getLogger(this::class)

    @Transactional
    fun håndterFerdigstilleVedtakUtenBeslutter(
        saksbehandling: Saksbehandling,
        sendTilBeslutterSteg: SendTilBeslutterSteg,
        beslutteVedtakSteg: BeslutteVedtakSteg,
        sendTilBeslutter: SendTilBeslutterDto?,
    ): Behandling {
        håndterSteg(saksbehandling, sendTilBeslutterSteg, sendTilBeslutter)
        val oppdatertBehandling = behandlingService.hentSaksbehandling(saksbehandling.id)
        val godkjentBesluttetVedtak = BeslutteVedtakDto(godkjent = true)
        return håndterSteg(oppdatertBehandling, beslutteVedtakSteg, godkjentBesluttetVedtak)
    }

    @Transactional
    fun resetSteg(
        behandlingId: UUID,
        steg: StegType,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != BehandlingStatus.UTREDES) {
            error("Kan ikke endre steg når status=${behandling.status} behandling=$behandlingId")
        }
        if (steg.kommerEtter(behandling.steg)) {
            error(
                "Kan ikke sette behandling til steg=$steg når behandling allerede " +
                    "er på ${behandling.steg} behandling=$behandlingId",
            )
        }

        validerAtStegKanResettes(behandling, steg)
        behandlingService.oppdaterStegPåBehandling(behandlingId, steg)
    }

    private fun validerAtStegKanResettes(
        behandling: Behandling,
        steg: StegType,
    ) {
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)
        val harTilgangTilNesteSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, steg.tillattFor)
        if (!harTilgangTilSteg || !harTilgangTilNesteSteg) {
            val saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
            error(
                "$saksbehandler kan ikke endre" +
                    " fra steg=${behandling.steg.displayName()} til steg=${steg.displayName()}" +
                    " pga manglende rolle på behandling=$behandling.id",
            )
        }
    }

    // Generelle stegmetoder
    fun <T> håndterSteg(
        saksbehandling: Saksbehandling,
        behandlingSteg: BehandlingSteg<T>,
        data: T,
    ): Behandling {
        val stegType = behandlingSteg.stegType()
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        try {
            valider(saksbehandling, stegType, saksbehandlerIdent, behandlingSteg)
            val nesteSteg = behandlingSteg.utførOgReturnerNesteSteg(saksbehandling, data)
            oppdaterHistorikk(behandlingSteg, saksbehandling.id, saksbehandlerIdent)
            validerNesteSteg(nesteSteg, saksbehandling)
            logger.info("$stegType på behandling ${saksbehandling.id} er håndtert")
            return behandlingService.oppdaterStegPåBehandling(behandlingId = saksbehandling.id, steg = nesteSteg)
        } catch (exception: Exception) {
            logger.warn("Håndtering av stegtype '$stegType' feilet på behandling ${saksbehandling.id}.")
            throw exception
        }
    }

    private fun validerNesteSteg(
        nesteSteg: StegType,
        saksbehandling: Saksbehandling,
    ) {
        if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingService.hentBehandling(saksbehandling.id).status)) {
            error(
                "Steg '${nesteSteg.displayName()}' kan ikke settes " +
                    "på behandling i kombinasjon med status ${saksbehandling.status}",
            )
        }
    }

    private fun <T> valider(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
        behandlingSteg: BehandlingSteg<T>,
    ) {
        utførBehandlingsvalidering(behandlingSteg, saksbehandling)
        validerHarTilgang(saksbehandling, stegType, saksbehandlerIdent)
        validerGyldigTilstand(saksbehandling, stegType, saksbehandlerIdent)
    }

    private fun <T> oppdaterHistorikk(
        behandlingSteg: BehandlingSteg<T>,
        behandlingId: UUID,
        saksbehandlerIdent: String,
    ) {
        if (behandlingSteg.settInnHistorikk()) {
            behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(
                    behandlingId = behandlingId,
                    steg = behandlingSteg.stegType(),
                    opprettetAvNavn = SikkerhetContext.hentSaksbehandlerNavn(),
                    opprettetAv = saksbehandlerIdent,
                ),
            )
        }
    }

    private fun <T> utførBehandlingsvalidering(
        behandlingSteg: BehandlingSteg<T>,
        saksbehandling: Saksbehandling,
    ) {
        behandlingSteg.validerSteg(saksbehandling)
        feilHvis(!behandlingSteg.stegType().erGyldigIKombinasjonMedStatus(saksbehandling.status)) {
            "Kan ikke utføre '${
                behandlingSteg.stegType().displayName()
            }' når behandlingstatus er ${saksbehandling.status.visningsnavn()}"
        }
    }

    private fun validerGyldigTilstand(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
    ) {
        if (saksbehandling.steg == BEHANDLING_FERDIGSTILT) {
            error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
        }

        if (stegType.kommerEtter(saksbehandling.steg)) {
            error(
                "$saksbehandlerIdent prøver å utføre steg '${stegType.displayName()}', " +
                    "men behandlingen er på steg '${saksbehandling.steg.displayName()}'",
            )
        }

        if (saksbehandling.steg == BESLUTTE_VEDTAK && stegType != BESLUTTE_VEDTAK) {
            error("Behandlingen er på steg '${saksbehandling.steg.displayName()}', og er da låst for alle andre type endringer.")
        }
    }

    private fun validerHarTilgang(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
    ) {
        val rolleForSteg: BehandlerRolle = utledRolleForSteg(stegType, saksbehandling)
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, rolleForSteg)

        logger.vanligInfo("Starter håndtering av $stegType på behandling ${saksbehandling.id}")
        logger.info(
            "Starter håndtering av $stegType på behandling " +
                "${saksbehandling.id} med saksbehandler=[$saksbehandlerIdent]",
        )

        feilHvis(!harTilgangTilSteg) {
            "$saksbehandlerIdent kan ikke utføre steg '${stegType.displayName()}' pga manglende rolle."
        }
    }

    private fun utledRolleForSteg(
        stegType: StegType,
        saksbehandling: Saksbehandling,
    ): BehandlerRolle {
        if (stegType == BESLUTTE_VEDTAK) {
            val vedtak = vedtakService.hentVedtak(saksbehandling.id)
            if (vedtak.erVedtakUtenBeslutter()) {
                return BehandlerRolle.SAKSBEHANDLER
            }
        }
        return saksbehandling.steg.tillattFor
    }
}
