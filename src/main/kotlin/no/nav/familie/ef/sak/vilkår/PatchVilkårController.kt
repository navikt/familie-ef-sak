package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.vilkår.regler.RegelId
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
                secureLogger.info("bygger opp nytt delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
                val gammelVilkårsvurderingEllerNull = eksisterendeVilkår.find { vilkår -> vilkår.type == VilkårType.INNTEKT }
                                                      ?: error("Mangler vilkårtype Inntekt for behandling med id=${it.id}")

                val nyDelvilkårsvurdering = lagNyDelvilkårsvurdering()
                if (liveRun.skalPersistere) {
                    secureLogger.info("persistererer delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
                    vilkårsvurderingRepository.update(gammelVilkårsvurderingEllerNull.copy(delvilkårsvurdering = DelvilkårsvurderingWrapper(
                            delvilkårsvurderinger = gammelVilkårsvurderingEllerNull.delvilkårsvurdering.delvilkårsvurderinger + nyDelvilkårsvurdering)))
                }
            }
        }
        return if (liveRun.skalPersistere) Ressurs.success("patching av inntektsvilkår ble kjørt") else Ressurs.success("patching av inntektsvilkår ble ikke kjørt")
    }

    private fun lagNyDelvilkårsvurdering(): Delvilkårsvurdering = Delvilkårsvurdering(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                                                      vurderinger = listOf(
                                                                                              Vurdering(regelId = RegelId.INNTEKT_SAMSVARER_MED_OS)))
}

data class LiveRun(val skalPersistere: Boolean)
