package no.nav.familie.ef.sak.vilkår.regler.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import java.util.UUID

class NyttBarnSammePartnerRegel :
    Vilkårsregel(
        vilkårType = VilkårType.NYTT_BARN_SAMME_PARTNER,
        regler = setOf(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
        hovedregler = regelIder(HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER),
    ) {
    @JsonIgnore
    private val logger = Logg.getLogger(this::class)

    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }
        logger.info("Initiering av nytt barn samme partner regel. Antall barn: ${metadata.barn.size} - barnId: $barnId")

        if (metadata.barn.size == 1) {
            return listOf(automatiskVurdertDelvilkår(RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER, SvarId.NEI, "Bruker har kun ett barn."))
        }
        if (kanAutomatiskInnvilgesForBrukerMedFlereBarn(metadata)) {
            return listOf(automatiskVurdertDelvilkår(RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER, SvarId.NEI, "Verken bruker eller annen forelder får eller har fått stønad for felles barn."))
        }

        return listOf(ubesvartDelvilkårsvurdering(RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER))
    }

    private fun kanAutomatiskInnvilgesForBrukerMedFlereBarn(metadata: HovedregelMetadata) =
        metadata.behandling.erSøknadSomBehandlingÅrsak() &&
            !metadata.vilkårgrunnlagDto.harBrukerEllerAnnenForelderTidligereVedtak() &&
            metadata.vilkårgrunnlagDto.barnMedSamvær.alleRegisterBarnHarRegistrertAnnenForelder()

    companion object {
        private val HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER =
            RegelSteg(
                regelId = RegelId.HAR_FÅTT_ELLER_VENTER_NYTT_BARN_MED_SAMME_PARTNER,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    ),
            )
    }
}

private fun Behandling.erSøknadSomBehandlingÅrsak() = this.årsak == BehandlingÅrsak.SØKNAD

private fun VilkårGrunnlagDto.harBrukerEllerAnnenForelderTidligereVedtak() =
    this.barnMedSamvær
        .filter { it.registergrunnlag.fødselsnummer != null }
        .mapNotNull { it.registergrunnlag.forelder?.tidligereVedtaksperioder }
        .any { it.harTidligereVedtaksperioder() } ||
        this.tidligereVedtaksperioder.harTidligereVedtaksperioder()

private fun List<BarnMedSamværDto>.alleRegisterBarnHarRegistrertAnnenForelder() =
    this
        .filter { it.registergrunnlag.fødselsnummer != null }
        .all { it.registergrunnlag.forelder?.fødselsnummer != null }
