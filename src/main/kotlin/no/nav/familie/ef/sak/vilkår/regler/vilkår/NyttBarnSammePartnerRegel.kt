package no.nav.familie.ef.sak.vilkår.regler.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.felles.util.norskFormat
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
import java.time.LocalDate
import java.util.UUID

class NyttBarnSammePartnerRegel : Vilkårsregel(
    vilkårType = VilkårType.NYTT_BARN_SAMME_PARTNER,
    regler = setOf(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
    hovedregler = regelIder(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER)
) {

    @JsonIgnore
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?
    ): List<Delvilkårsvurdering> {
        logger.info("Initiering av nytt barn samme partner regel. Antall barn: ${metadata.barn.size} - barnId: $barnId - terminbarn: ${ingenTerminbarn(metadata.barn)}")
        if (metadata.erSøknadSomBehandlingÅrsak && metadata.barn.size == 1 && ingenTerminbarn(metadata.barn)) {
            return listOf(
                Delvilkårsvurdering(
                    resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
                    listOf(
                        Vurdering(
                            regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                            svar = SvarId.NEI,
                            begrunnelse = "Automatisk vurdert (${LocalDate.now().norskFormat()}): Bruker har kun ett barn og det er ikke oppgitt nytt terminbarn i søknaden."
                        )
                    )
                )
            )
        } else if (metadata.erSøknadSomBehandlingÅrsak && !metadata.harBrukerEllerAnnenForelderTidligereVedtak) {
            return listOf(
                Delvilkårsvurdering(
                    resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
                    listOf(
                        Vurdering(
                            regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                            svar = SvarId.NEI,
                            begrunnelse = "Automatisk vurdert (${LocalDate.now().norskFormat()}): Verken bruker eller annen forelder får eller har fått stønad for felles barn."
                        )
                    )
                )
            )
        }
        return listOf(ubesvartDelvilkårsvurdering(RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER))
    }

    private fun ingenTerminbarn(barn: List<BehandlingBarn>) =
        barn.none { it.fødselTermindato != null && it.personIdent == null }

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
