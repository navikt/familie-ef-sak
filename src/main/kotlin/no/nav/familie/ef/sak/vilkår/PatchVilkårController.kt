package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/patch-vilkar"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
@Validated
class PatchVilkårController(
    private val patchVilkårRepository: PatchVilkårRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseService: TilkjentYtelseService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("inntekt")
    fun patchInntekt(@RequestBody liveRun: LiveRun): Ressurs<String> {

        val behandlinger = patchVilkårRepository.finnBehandlingerSomHarBarnetilsyn()
        behandlinger.forEach {
            val eksisterendeVilkår = vilkårsvurderingRepository.findByBehandlingId(it.id)

            if (eksisterendeVilkår.flatMap { it.delvilkårsvurdering.delvilkårsvurderinger.flatMap { it.vurderinger.map { it.regelId } } }
                .contains(RegelId.INNTEKT_SAMSVARER_MED_OS)
            ) {
                logger.info("det finnes allerede et delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
            } else {
                logger.info("bygger opp nytt delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
                val gammelVilkårsvurderingEllerNull = eksisterendeVilkår.find { vilkår -> vilkår.type == VilkårType.INNTEKT }

                if (gammelVilkårsvurderingEllerNull == null) {
                    logger.info("behandling med id=${it.id} har ikke inntektsvilkår")
                } else {
                    val nyDelvilkårsvurdering = lagNyDelvilkårsvurdering(it.id)
                    if (liveRun.skalPersistere) {
                        logger.info("persistererer delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
                        vilkårsvurderingRepository.update(
                            gammelVilkårsvurderingEllerNull.copy(
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(
                                    delvilkårsvurderinger = gammelVilkårsvurderingEllerNull.delvilkårsvurdering.delvilkårsvurderinger + nyDelvilkårsvurdering
                                )
                            )
                        )
                    }
                }
            }
        }
        return if (liveRun.skalPersistere) Ressurs.success("patching av inntektsvilkår ble kjørt") else Ressurs.success("patching av inntektsvilkår ble ikke kjørt")
    }

    private fun lagNyDelvilkårsvurdering(behandlingId: UUID): Delvilkårsvurdering {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsak.fagsakPersonId)
        val sisteIverksatteBehandling =
            fagsaker.overgangsstønad?.let { behandlingRepository.finnSisteIverksatteBehandling(it.id) }
        val svar = utledSvar(sisteIverksatteBehandling)

        return Delvilkårsvurdering(
            resultat = Vilkårsresultat.OPPFYLT,
            vurderinger = listOf(
                Vurdering(
                    regelId = RegelId.INNTEKT_SAMSVARER_MED_OS,
                    svar = svar,
                    begrunnelse = "Nytt delvilkår - automatisk lagt inn i ettertid"
                )
            )
        )
    }

    private fun utledSvar(behandling: Behandling?): SvarId {
        if (behandling != null) {
            if (tilkjentYtelseService.harLøpendeUtbetaling(behandling.id)) {
                return SvarId.JA
            }
        }
        return SvarId.BRUKER_MOTTAR_IKKE_OVERGANGSSTØNAD
    }
}

data class LiveRun(val skalPersistere: Boolean)
