package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Unprotected // kommer kunne brukes uten token
@RestController
@RequestMapping("api/g-omregning-opprydding")
class GOmregningOppryddingController(
    private val gOmregningOppryddingService: GOmregningOppryddingService,
) {

    @PostMapping("{fagsakId}")
    fun migrerAutomatiskt(@PathVariable("fagsakId") fagsakId: UUID) {
        gOmregningOppryddingService.ryddOppGOmregninger(fagsakId)
    }

}

@Service
class GOmregningOppryddingService(
    private val søknadService: SøknadService,
    private val barnService: BarnService,
    private val vurderingService: VurderingService,
    private val vurderingRepository: VilkårsvurderingRepository,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val behandlingService: BehandlingService,
    private val behandlingRepository: BehandlingRepository
) {

    @Transactional
    fun ryddOppGOmregninger(fagsakId: UUID) {
        val behandling = behandlingRepository.findByFagsakIdAndÅrsak(fagsakId, BehandlingÅrsak.G_OMREGNING)
        ryddOppGOmregninger(behandlingService.hentSaksbehandling(behandling.id))
    }

    @Transactional
    fun ryddOppGOmregninger(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        val forrigeBehandlingId = saksbehandling.forrigeBehandlingId
            ?: error("Finner ikke forrigeBehandlingId til $behandlingId")
        søknadService.kopierSøknad(forrigeBehandlingId, behandlingId)

        val (_, metadata) = vurderingService.hentGrunnlagOgMetadata(behandlingId)
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        barnService.opprettBarnForRevurdering(
            behandlingId = behandlingId,
            forrigeBehandlingId = forrigeBehandlingId,
            nyeBarnPåRevurdering = emptyList(),
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = saksbehandling.stønadstype
        )

        vurderingRepository.deleteAllByBehandlingId(behandlingId)
        vurderingService.kopierVurderingerTilNyBehandling(
            forrigeBehandlingId,
            behandlingId,
            metadata,
            saksbehandling.stønadstype
        )
    }
}