package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.NullstillVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.OppdaterVilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.regler.Vilkårsregler
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping(path = ["/api/vurdering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VurderingController(private val vurderingService: VurderingService,
                          private val stegService: StegService,
                          private val behandlingService: BehandlingService,
                          private val tilgangService: TilgangService) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping("regler")
    fun hentRegler(): Ressurs<Vilkårsregler> {
        return Ressurs.success(Vilkårsregler.VILKÅRSREGLER)
    }

    @PostMapping("vilkar")
    fun oppdaterVurderingVilkår(@RequestBody vilkårsvurdering: OppdaterVilkårsvurderingDto)
            : Ressurs<VilkårsvurderingDto> {
        tilgangService.validerTilgangTilBehandling(vilkårsvurdering.behandlingId)
        try {
            return Ressurs.success(vurderingService.oppdaterVilkår(vilkårsvurdering))
        } catch (e: Exception) {
            val delvilkårJson = objectMapper.writeValueAsString(vilkårsvurdering.delvilkårsvurderinger)
            secureLogger.warn("id=${vilkårsvurdering.id}" +
                              " behandlingId=${vilkårsvurdering.behandlingId}" +
                              " svar=$delvilkårJson")
            throw e
        }
    }

    @PostMapping("nullstill")
    fun nullstillVilkår(@RequestBody request: NullstillVilkårsvurderingDto): Ressurs<VilkårsvurderingDto> {
        tilgangService.validerTilgangTilBehandling(request.behandlingId)
        return Ressurs.success(vurderingService.nullstillVilkår(request))
    }

    @GetMapping("{behandlingId}/vilkar")
    fun getVilkår(@PathVariable behandlingId: UUID): Ressurs<VilkårDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(vurderingService.hentVilkår(behandlingId))
    }

    @PostMapping("/{behandlingId}/vilkar/fullfor")
    fun fullførVilkår(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        // TODO; Trenger vi registrer opplysninger?
        val oppdatertBehandling = stegService.håndterRegistrerOpplysninger(behandling, null)
        return Ressurs.success(stegService.håndterInngangsvilkår(oppdatertBehandling).id)
    }

    @PostMapping("/{behandlingId}/overgangsstonad/fullfor")
    fun fullførStønadsvilkår(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterStønadsvilkår(behandling).id)
    }
}
