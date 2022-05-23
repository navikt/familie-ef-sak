package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
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
class PatchVilkårController(private val patchVilkårRepository: PatchVilkårRepository,
                            private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                            private val fagsakService: FagsakService,
                            private val fagsakPersonService: FagsakPersonService) {

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

                if (gammelVilkårsvurderingEllerNull != null) {
                    val nyDelvilkårsvurdering = lagNyDelvilkårsvurdering(it.id)
                    if (liveRun.skalPersistere) {
                        secureLogger.info("persistererer delvilkår for om inntekt samsvarer med OS på behandling med id=${it.id}")
                        vilkårsvurderingRepository.update(gammelVilkårsvurderingEllerNull.copy(delvilkårsvurdering = DelvilkårsvurderingWrapper(
                                delvilkårsvurderinger = gammelVilkårsvurderingEllerNull.delvilkårsvurdering.delvilkårsvurderinger.plusElement(
                                        nyDelvilkårsvurdering))))
                    }
                } else {
                    secureLogger.info("persisterer ikke delvilkår - fantes ingen vilkår for inntekt på behandling med id=${it.id}")
                }
            }
        }
        return if (liveRun.skalPersistere) Ressurs.success("patching av inntektsvilkår ble kjørt") else Ressurs.success("patching av inntektsvilkår ble ikke kjørt")
    }

    private fun lagNyDelvilkårsvurdering(id: UUID): Delvilkårsvurdering {
        val fagsak = fagsakService.hentFagsakForBehandling(id)
        val person = fagsakPersonService.hentPerson(fagsak.fagsakPersonId)
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(person.id)
        val svar = if (fagsaker.overgangsstønad != null) SvarId.JA else SvarId.BRUKER_MOTTAR_IKKE_OVERGANGSSTØNAD

        return Delvilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT,
                                   vurderinger = listOf(Vurdering(regelId = RegelId.INNTEKT_SAMSVARER_MED_OS,
                                                                  svar = svar,
                                                                  begrunnelse = "Nytt delvilkår - automatisk lagt inn i ettertid")))
    }
}

data class LiveRun(val skalPersistere: Boolean)
