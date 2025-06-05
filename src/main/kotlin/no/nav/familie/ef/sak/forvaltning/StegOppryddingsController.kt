package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/steg-forvaltning/"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class StegOppryddingsController(
    private val iverksettingDtoMapper: IverksettingDtoMapper,
    private val behandlingService: BehandlingService,
    private val iverksettClient: IverksettClient,
    private val tilgangService: TilgangService,
    private val brevRepository: VedtaksbrevRepository,
) {
    @PostMapping("iverksettOgOppdaterStatus/behandling/{behandlingId}")
    fun sjekkIdenter(
        @PathVariable behandlingId: UUID,
    ) {
        tilgangService.validerHarForvalterrolle()
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)

        val beslutterident = vedtaksbrev.beslutterident

        feilHvis(beslutterident == null) {
            "Behandlingen har ikke beslutterident. BehandlingId: $behandlingId"
        }

        val iverksettDto =
            iverksettingDtoMapper.tilDto(
                saksbehandling = saksbehandling,
                beslutter = beslutterident,
            )

        val beslutterPdf = vedtaksbrev.beslutterPdf

        feilHvis(beslutterPdf == null) {
            "Behandlingen har ikke beslutterPdf. BehandlingId: $behandlingId"
        }

        iverksettClient.iverksett(iverksettDto = iverksettDto, fil = Fil(bytes = beslutterPdf.bytes))
        behandlingService.oppdaterStegPåBehandling(behandlingId = saksbehandling.id, steg = StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
    }
}
