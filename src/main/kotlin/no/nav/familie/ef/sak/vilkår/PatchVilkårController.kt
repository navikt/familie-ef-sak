package no.nav.familie.ef.sak.vilkår

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


@RestController
@RequestMapping(path = ["/api/patch-vilkar"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
@Validated
class PatchVilkårController(private val patchVilkårRepository: PatchVilkårRepository,
                            private val vilkårsvurderingRepository: VilkårsvurderingRepository) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping("inntekt")
    fun patchInntekt(@RequestBody liveRun: LiveRun)
            : Ressurs<String> {

        val behandlinger = patchVilkårRepository.finnBehandlingerSomHarBarnetilsyn()
        behandlinger.forEach {
            val eksisterendeVilkår = vilkårsvurderingRepository.findByBehandlingId(it.id)

            if (eksisterendeVilkår.flatMap { it.delvilkårsvurdering.delvilkårsvurderinger.flatMap { it.vurderinger.map { it.regelId } } }
                            .contains(RegelId.INNTEKT_SAMSVARER_MED_OS)) {
                secureLogger.info("det finnes allerede et delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
            } else {
                secureLogger.info("finner ut om vilkår for inntekt finnes fra før på behandling med id=${it.id}")
                val gammelVilkårsvurdering = eksisterendeVilkår.find { it.type == VilkårType.INNTEKT }

                if (liveRun.skalPersistere) {
                    if (gammelVilkårsvurdering != null) {
                        val nyDelvilkårsvurdering = lagNyDelvilkårsvurdering()
                        val oppdatertVilkårsvurdering =
                                lagOppdatertVilkårsvurdering(gammelVilkårsvurdering, nyDelvilkårsvurdering)
                        secureLogger.info("persistererer delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
                        vilkårsvurderingRepository.insert(oppdatertVilkårsvurdering)
                    } else {
                        secureLogger.info("persistererer ikke delvilkår - vilkåret for inntekt fantes ikke fra før på behandling med id=${it.id}")
                    }
                }
            }
        }
        return if (liveRun.skalPersistere) Ressurs.success("patching av inntektsvilkår ble kjørt") else Ressurs.success("patching av inntektsvilkår ble ikke kjørt")
    }

    private fun lagOppdatertVilkårsvurdering(gammel: Vilkårsvurdering, ny: Delvilkårsvurdering): Vilkårsvurdering {
        return Vilkårsvurdering(behandlingId = gammel.behandlingId,
                                resultat = gammel.resultat,
                                type = gammel.type,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger = gammel.delvilkårsvurdering.delvilkårsvurderinger.plusElement(
                                        ny)))
    }

    private fun lagNyDelvilkårsvurdering(): Delvilkårsvurdering = Delvilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                                                                      vurderinger = listOf(Vurdering(regelId = RegelId.INNTEKT_SAMSVARER_MED_OS,
                                                                                                                     svar = SvarId.JA,
                                                                                                                     begrunnelse = "")))
}

data class LiveRun(val skalPersistere: Boolean)
