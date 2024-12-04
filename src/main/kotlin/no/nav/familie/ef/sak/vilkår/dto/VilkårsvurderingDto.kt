package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import java.time.LocalDateTime
import java.util.UUID

data class VilkårsvurderingDto(
    val id: UUID,
    val behandlingId: UUID,
    val resultat: Vilkårsresultat,
    val vilkårType: VilkårType,
    val barnId: UUID? = null,
    val endretAv: String,
    val endretTid: LocalDateTime,
    val delvilkårsvurderinger: List<DelvilkårsvurderingDto> = emptyList(),
    val opphavsvilkår: OpphavsvilkårDto?,
    val kanGjenbrukes: Boolean,
)

data class OpphavsvilkårDto(
    val behandlingId: UUID,
    val endretTid: LocalDateTime,
)

data class OppdaterVilkårsvurderingDto(
    val id: UUID,
    val behandlingId: UUID,
)

data class SvarPåVurderingerDto(
    val id: UUID,
    val behandlingId: UUID,
    val delvilkårsvurderinger: List<DelvilkårsvurderingDto>,
)

data class GjenbrukVilkårsvurderingerDto(
    val behandlingId: UUID,
    val kopierBehandlingId: UUID,
)

data class EnkeltVilkårForGjenbrukRequest(
    val behandlingId: UUID,
    val vilkårId: UUID,
)

data class DelvilkårsvurderingDto(
    val resultat: Vilkårsresultat,
    val vurderinger: List<VurderingDto>,
) {
    /**
     * @return regelId for første svaret som er hovedregeln på delvilkåret
     */
    fun hovedregel() = this.vurderinger.first().regelId
}

data class VurderingDto(
    val regelId: RegelId,
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
)

fun Vurdering.tilDto() = VurderingDto(this.regelId, this.svar, this.begrunnelse)

fun Delvilkårsvurdering.tilDto() = DelvilkårsvurderingDto(this.resultat, this.vurderinger.map { it.tilDto() })

fun Vilkårsvurdering.tilDto(kanGjenbrukes: Boolean = false) =
    VilkårsvurderingDto(
        id = this.id,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        vilkårType = this.type,
        barnId = this.barnId,
        endretAv = this.sporbar.endret.endretAv,
        endretTid = this.sporbar.endret.endretTid,
        delvilkårsvurderinger =
            this.delvilkårsvurdering.delvilkårsvurderinger
                .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
                .map { it.tilDto() },
        opphavsvilkår = this.opphavsvilkår?.let { OpphavsvilkårDto(it.behandlingId, it.vurderingstidspunkt) },
        kanGjenbrukes = kanGjenbrukes,
    )

fun DelvilkårsvurderingDto.svarTilDomene() = this.vurderinger.map { it.tilDomene() }

fun VurderingDto.tilDomene() = Vurdering(this.regelId, this.svar, this.begrunnelse)
