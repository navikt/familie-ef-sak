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
class PatchVilkårController(
    private val patchVilkårRepository: PatchVilkårRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping("dokumentasjon-tilsynsutgifter")
    fun patchDokumentasjonTilsynsutgifter(@RequestBody liveRun: LiveRun): Ressurs<String> {

        val behandlinger = patchVilkårRepository.finnBehandlingerSomHarBarnetilsyn()
        behandlinger.forEach {
            val eksisterendeVilkår = vilkårsvurderingRepository.findByBehandlingId(it.id)
            if (eksisterendeVilkår.map { it.type }.contains(VilkårType.DOKUMENTASJON_TILSYNSUTGIFTER)) {
                secureLogger.info("det finnes allerede et vilkår for dokumentasjon av tilsynsutgifter på behandling med id=${it.id}")
            } else {
                secureLogger.info("oppretter vilkår for dokumentasjon av tilsynsutgifter på behandling med id=${it.id}")
                val nyVilkårsvurdering = Vilkårsvurdering(
                    behandlingId = it.id,
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.DOKUMENTASJON_TILSYNSUTGIFTER,
                    delvilkårsvurdering = DelvilkårsvurderingWrapper(
                        delvilkårsvurderinger = listOf(
                            Delvilkårsvurdering(
                                resultat = Vilkårsresultat.OPPFYLT,
                                vurderinger = listOf(
                                    Vurdering(
                                        regelId = RegelId.HAR_DOKUMENTERTE_TILSYNSUTGIFTER,
                                        svar = SvarId.JA,
                                        begrunnelse = ""
                                    )
                                )
                            )
                        )
                    )
                )
                if (liveRun.skalPersistere) {
                    secureLogger.info("persistererer vilkår for dokumentasjon av tilsynsutgifter på behandling med id=${it.id}")
                    vilkårsvurderingRepository.insert(nyVilkårsvurdering)
                }
            }
        }
        return Ressurs.success("patching av dokumentasjonTilsynsutgifter ble kjørt")
    }
}

data class LiveRun(val skalPersistere: Boolean)
