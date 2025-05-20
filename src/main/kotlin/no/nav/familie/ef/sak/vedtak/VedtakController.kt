package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegServiceDeprecated
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.ef.sak.vedtak.dto.TotrinnskontrollStatusDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakController(
    private val stegServiceDeprecated: StegServiceDeprecated,
    private val behandlingService: BehandlingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
    private val vurderingService: VurderingService,
    private val vedtakHistorikkService: VedtakHistorikkService,
    private val behandlingRepository: BehandlingRepository,
    private val nullstillVedtakService: NullstillVedtakService,
    private val angreSendTilBeslutterService: AngreSendTilBeslutterService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val environment: org.springframework.core.env.Environment,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/{behandlingId}/send-til-beslutter")
    fun sendTilBeslutter(
        @PathVariable behandlingId: UUID,
        @RequestBody sendTilBeslutter: SendTilBeslutterDto?,
    ): Ressurs<UUID> {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandling, AuditLoggerEvent.UPDATE)

        validerTilordnetRessurs(behandlingId)

        val vedtakErUtenBeslutter = vedtakService.hentVedtak(behandlingId).utledVedtakErUtenBeslutter()

        return if (vedtakErUtenBeslutter.value) {
            Ressurs.success(stegServiceDeprecated.håndterFerdigstilleVedtakUtenBeslutter(behandling, sendTilBeslutter).id)
        } else {
            Ressurs.success(stegServiceDeprecated.håndterSendTilBeslutter(behandling, sendTilBeslutter).id)
        }
    }

    @PostMapping("/{behandlingId}/angre-send-til-beslutter")
    fun angreSendTilBeslutter(
        @PathVariable behandlingId: UUID,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        angreSendTilBeslutterService.angreSendTilBeslutter(behandlingId)
        return Ressurs.success(behandlingId)
    }

    @PostMapping("/{behandlingId}/beslutte-vedtak")
    fun beslutteVedtak(
        @PathVariable behandlingId: UUID,
        @RequestBody request: BeslutteVedtakDto,
    ): Ressurs<UUID> {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandling, AuditLoggerEvent.UPDATE)
        validerTilordnetRessurs(
            behandlingId,
            setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak, Oppgavetype.GodkjenneVedtak),
        )
        if (!request.godkjent && request.begrunnelse.isNullOrBlank()) {
            throw ApiFeil("Mangler begrunnelse", HttpStatus.BAD_REQUEST)
        }
        return Ressurs.success(stegServiceDeprecated.håndterBeslutteVedtak(behandling, request).id)
    }

    @GetMapping("{behandlingId}/totrinnskontroll")
    fun hentTotrinnskontroll(
        @PathVariable behandlingId: UUID,
    ): ResponseEntity<Ressurs<TotrinnskontrollStatusDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId)
        return ResponseEntity.ok(Ressurs.success(totrinnskontroll))
    }

    @GetMapping("{behandlingId}")
    fun hentVedtak(
        @PathVariable behandlingId: UUID,
    ): Ressurs<VedtakDto?> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(vedtakService.hentVedtakDtoHvisEksisterer(behandlingId))
    }

    @GetMapping("{behandlingId}/historikk/{fra}")
    fun hentVedtakForBehandling(
        @PathVariable behandlingId: UUID,
        @PathVariable fra: YearMonth,
    ): Ressurs<VedtakDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(vedtakHistorikkService.hentVedtakFraDato(behandlingId, fra))
    }

    @PostMapping("/{behandlingId}/lagre-vedtak")
    fun lagreVedtak(
        @PathVariable behandlingId: UUID,
        @RequestBody vedtak: VedtakDto,
    ): Ressurs<UUID> {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        validerKanRedigereBehandling(behandling)
        validerGyldigAvslagÅrsak(vedtak)
        validerAlleVilkårOppfyltDersomInvilgelse(vedtak, behandlingId)
        return Ressurs.success(stegServiceDeprecated.håndterBeregnYtelseForStønad(behandling, vedtak).id)
    }

    @DeleteMapping("/{behandlingId}")
    fun nullstillVedtak(
        @PathVariable behandlingId: UUID,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.DELETE)
        tilgangService.validerHarSaksbehandlerrolle()

        nullstillVedtakService.nullstillVedtak(behandlingId)
        return Ressurs.success(behandlingId)
    }

    private fun validerAlleVilkårOppfyltDersomInvilgelse(
        vedtak: VedtakDto,
        behandlingId: UUID,
    ) {
        if (vedtak is InnvilgelseOvergangsstønad) {
            brukerfeilHvisIkke(vurderingService.erAlleVilkårOppfylt(behandlingId)) {
                "Kan ikke fullføre en behandling med resultat innvilget hvis ikke alle vilkår er oppfylt"
            }
        }
    }

    private fun validerKanRedigereBehandling(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og vedtaket kan derfor ikke lagres"
        }
        validerTilordnetRessurs(saksbehandling.id)
    }

    private fun validerGyldigAvslagÅrsak(vedtak: VedtakDto) {
        if (vedtak is Avslå) {
            brukerfeilHvis(!vedtak.erGydlig()) { "Ikke gyldig avslagsgrunn" }
        }
    }

    private fun validerTilordnetRessurs(
        behandlingId: UUID,
        oppgavetyper: Set<Oppgavetype> =
            setOf(
                Oppgavetype.BehandleSak,
                Oppgavetype.BehandleUnderkjentVedtak,
            ),
    ) {
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId, oppgavetyper)) {
            "Behandlingen har en annen eier og du kan derfor ikke lagre vedtaket"
        }
    }

    @GetMapping("/eksternid/{eksternId}/inntekt")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"]) // Familie-ef-personhendelse bruker denne
    fun hentForventetInntektForEksternId(
        @PathVariable eksternId: Long,
        dato: LocalDate?,
    ): Ressurs<Int?> {
        val behandlingId = behandlingService.hentBehandlingPåEksternId(eksternId).id

        val forventetInntekt = vedtakService.hentForventetInntektForBehandlingIds(behandlingId, dato ?: LocalDate.now())
        return Ressurs.success(forventetInntekt)
    }

    @GetMapping("/eksternid/{eksternId}/harAktivtVedtak")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"]) // Familie-ef-personhendelse bruker denne
    fun hentHarAktivStonad(
        @PathVariable eksternId: Long,
        dato: LocalDate?,
    ): Ressurs<Boolean> {
        val behandlingId = behandlingService.hentBehandlingPåEksternId(eksternId).id

        val forventetInntekt = vedtakService.hentHarAktivtVedtak(behandlingId, dato ?: LocalDate.now())
        return Ressurs.success(forventetInntekt)
    }

    @GetMapping("/personerMedAktivStonadIkkeManueltRevurdertSisteMaaneder")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"]) // Familie-ef-personhendelse bruker denne
    fun hentPersonerMedAktivStonadIkkeManueltRevurdertSisteToMåneder(
        @RequestParam antallMaaneder: Int = 3,
    ): Ressurs<List<String>> =
        if (environment.activeProfiles.contains("prod")) {
            Ressurs.success(behandlingRepository.finnPersonerMedAktivStonadIkkeRevurdertSisteMåneder(antallMåneder = antallMaaneder))
        } else {
            Ressurs.success(behandlingRepository.finnPersonerMedAktivStonadIkkeRevurdertSisteMåneder(antallMåneder = 0))
        }

    @PostMapping("/gjeldendeIverksatteBehandlingerMedInntekt")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"]) // Familie-ef-personhendelse bruker denne
    fun hentPersonerMedAktivStonadOgForventetInntekt(
        @RequestBody
        personIdenter: List<String>,
    ): Ressurs<List<ForventetInntektForPersonIdent>> {
        logger.info("hentPersonerMedAktivStonadOgForventetInntekt start")
        val personIdentToBehandlingIds =
            behandlingRepository.finnSisteIverksatteBehandlingerForPersonIdenter(personIdenter).toMap()
        logger.info("hentPersonerMedAktivStonadOgForventetInntekt hentet behandlinger")

        val personIdentMedForventetInntektList = mutableListOf<PersonIdentMedForventetInntekt>()
        val behandlingIdToForventetInntektMap =
            vedtakService.hentForventetInntektForBehandlingIds(personIdentToBehandlingIds.values)

        for (personIdent in personIdentToBehandlingIds.keys) {
            val behandlingId = personIdentToBehandlingIds[personIdent]
            val forventetInntektForBehandling = behandlingIdToForventetInntektMap[behandlingId]
            secureLogger.info("PersonIdent: $personIdent ForventetInntektForBehandling: $forventetInntektForBehandling")
            if (forventetInntektForBehandling == null) {
                secureLogger.warn("Fant ikke behandling $behandlingId knyttet til ident $personIdent - får ikke vurdert inntekt")
            } else {
                personIdentMedForventetInntektList.add(PersonIdentMedForventetInntekt(personIdent, forventetInntektForBehandling))
            }
        }

        logger.info("hentPersonerMedAktivStonadOgForventetInntekt done")
        return Ressurs.success(
            personIdentMedForventetInntektList.map {
                ForventetInntektForPersonIdent(
                    it.personIdent,
                    it.forventetInntektForMåned.forventetInntektForrigeMåned,
                    it.forventetInntektForMåned.forventetInntektToMånederTilbake,
                    it.forventetInntektForMåned.forventetInntektTreMånederTilbake,
                    it.forventetInntektForMåned.forventetInntektFireMånederTilbake,
                )
            },
        )
    }
}
