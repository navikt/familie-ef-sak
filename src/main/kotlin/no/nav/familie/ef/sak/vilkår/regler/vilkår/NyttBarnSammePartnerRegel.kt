package no.nav.familie.ef.sak.vilkår.regler.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import org.slf4j.LoggerFactory
import java.util.*

class NyttBarnSammePartnerRegel : Vilkårsregel(
    vilkårType = VilkårType.NYTT_BARN_SAMME_PARTNER,
    regler = setOf(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
    hovedregler = regelIder(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER)
) {

    @JsonIgnore
    private val logger = LoggerFactory.getLogger(javaClass)

    @JsonIgnore
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?
    ): List<Delvilkårsvurdering> {
        logger.info("Initiering av nytt barn samme partner regel. Antall barn: ${metadata.barn.size} - barnId: $barnId - terminbarn: ${metadata.terminbarnISøknad}")
        if (metadata.barn.size == 1 && !metadata.terminbarnISøknad) {
            return listOf(
                Delvilkårsvurdering(
                    resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
                    listOf(
                        Vurdering(
                            regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                            svar = SvarId.NEI,
                            begrunnelse = "Automatisk vurdert: Ut ifra at bruker har kun ett barn og at det ikke er oppgitt noen terminbarn i søknad, vurderes vilkåret til oppfylt"
                        )
                    )
                )
            )
        } else if (!metadata.harBrukerEllerAnnenForelderTidligereVedtak) {
            return listOf(
                Delvilkårsvurdering(
                    resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
                    listOf(
                        Vurdering(
                            regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                            svar = SvarId.NEI,
                            begrunnelse = "Automatisk vurdert: Hverken bruker eller annen forelder har mottatt stønad tidligere."
                        )
                    )
                )
            )
        }
        return listOf(
            Delvilkårsvurdering(
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                vurderinger = listOf(
                    Vurdering(
                        regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                        svar = null,
                        begrunnelse = null
                    )
                )
            )
        )
    }

    companion object {

        private val HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER =
            RegelSteg(
                regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
                )
            )
    }
}
