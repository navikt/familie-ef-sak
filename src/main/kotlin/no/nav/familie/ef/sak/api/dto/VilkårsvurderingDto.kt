package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.VilkårSvar
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import java.time.LocalDateTime
import java.util.UUID

data class VilkårsvurderingDto(val id: UUID,
                               val behandlingId: UUID,
                               val resultat: Vilkårsresultat,
                               val vilkårType: VilkårType,
                               val barnId: UUID? = null,
                               val endretAv: String,
                               val endretTid: LocalDateTime,
                               val delvilkårsvurderinger: List<DelvilkårsvurderingDto> = emptyList())

data class OppdaterVilkårsvurderingDto(val id: UUID,
                                       val behandlingId: UUID,
                                       val delvilkårsvurderinger: List<DelvilkårsvurderingDto> = emptyList())

data class DelvilkårsvurderingDto(val type: RegelId,
                                  val svar: List<VilkårSvarDto>)

data class VilkårSvarDto(val regelId: RegelId,
                         val svar: SvarId?,
                         val begrunnelse: String?)

fun VilkårSvar.tilDto() = VilkårSvarDto(this.regelId, this.svar, this.begrunnelse)

fun Delvilkårsvurdering.tilDto() =
        DelvilkårsvurderingDto(this.type, this.svar.map { it.tilDto() })

fun Vilkårsvurdering.tilDto() =
        VilkårsvurderingDto(id = this.id,
                            behandlingId = this.behandlingId,
                            resultat = this.resultat,
                            vilkårType = this.type,
                            barnId = this.barnId,
                            endretAv = this.sporbar.endret.endretAv,
                            endretTid = this.sporbar.endret.endretTid,
                            delvilkårsvurderinger = this.delvilkårsvurdering.delvilkårsvurderinger.map { it.tilDto() })

fun DelvilkårsvurderingDto.svarTilDomene() = this.svar.map { it.tilDomene() }
fun VilkårSvarDto.tilDomene() = VilkårSvar(this.regelId, this.svar, this.begrunnelse)