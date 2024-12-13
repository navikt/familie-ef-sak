package no.nav.familie.ef.sak.behandling.henlegg
import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
class HenleggBehandlingController(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val henleggService: HenleggService,
    private val tilgangService: TilgangService,
    private val featureToggleService: FeatureToggleService,
    private val taskService: TaskService,
    private val personopplysningerService: PersonopplysningerService,
) {
    @GetMapping("/{behandlingId}/henlegg/brev/forhandsvisning")
    fun genererHenleggBrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.ACCESS)
        return henleggBrevRessurs(behandlingId)
    }

    private fun henleggBrevRessurs(
        behandlingId: UUID,
    ) = Ressurs.success(henleggService.genererHenleggelsesbrev(behandlingId))

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(
        @PathVariable behandlingId: UUID,
        @RequestBody henlagt: HenlagtDto,
    ): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        val henlagtBehandling = henleggService.henleggBehandling(behandlingId, henlagt)
        val fagsak: Fagsak = fagsakService.hentFagsak(henlagtBehandling.fagsakId)
        if (henlagt.skalSendeHenleggelsesbrev) {
            sendHenleggelsesbrev(henlagt, behandlingId)
        }

        return Ressurs.success(henlagtBehandling.tilDto(fagsak.stønadstype))
    }

    private fun sendHenleggelsesbrev(
        henlagt: HenlagtDto,
        behandlingId: UUID,
    ) {
        validerIkkeSendBrevPåFeilType(henlagt)
        validerIkkeSendBrevHvisVergemålEllerFullmakt(behandlingService.hentAktivIdent(behandlingId))
        val saksbehandlerSignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        val task: Task =
            SendTrukketSøknadHenleggelsesbrevTask.opprettTask(behandlingId, saksbehandlerSignatur, saksbehandlerIdent)

        taskService.save(task)
    }

    private fun validerIkkeSendBrevPåFeilType(henlagt: HenlagtDto) {
        feilHvis(henlagt.skalSendeHenleggelsesbrev && henlagt.årsak == HenlagtÅrsak.FEILREGISTRERT) { "Skal ikke sende brev hvis type er ulik trukket tilbake" }
    }

    private fun validerIkkeSendBrevHvisVergemålEllerFullmakt(
        ident: String,
    ) {
        val personopplysninger = personopplysningerService.hentPersonopplysningerFraRegister(ident)
        val harVerge = personopplysninger.vergemål.isNotEmpty()
        val harFullmakt: Boolean =
            personopplysninger.fullmakt
                .filter {
                    it.gyldigTilOgMed == null ||
                        (
                            it.gyldigTilOgMed.isEqualOrAfter(
                                LocalDate.now(),
                            )
                        )
                }.isNotEmpty()
        feilHvis(harVerge || harFullmakt) {
            "Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt"
        }
    }

    @PostMapping("{behandlingId}/henlegg/behandling-uten-oppgave")
    fun henleggBehandlingUtenOppgave(
        @PathVariable behandlingId: UUID,
        @RequestBody henlagt: HenlagtDto,
    ): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        feilHvis(!featureToggleService.isEnabled(toggle = Toggle.HENLEGG_BEHANDLING_UTEN_OPPGAVE)) {
            "Henleggelse av behandling uten å henlegge oppgave er ikke mulig - toggle ikke enablet for bruker"
        }

        val henlagtBehandling = henleggService.henleggBehandlingUtenOppgave(behandlingId, henlagt)
        val fagsak: Fagsak = fagsakService.hentFagsak(henlagtBehandling.fagsakId)
        return Ressurs.success(henlagtBehandling.tilDto(fagsak.stønadstype))
    }
}
