package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.DelvilkårType
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import java.time.LocalDateTime
import java.util.*

data class VilkårsvurderingDto(val id: UUID,
                               val behandlingId: UUID,
                               val resultat: Vilkårsresultat,
                               val vilkårType: VilkårType,
                               val begrunnelse: String? = null,
                               val unntak: String? = null,
                               val endretAv: String,
                               val endretTid: LocalDateTime,
                               val delvilkårsvurderinger: List<DelvilkårsvurderingDto> = emptyList())

data class DelvilkårsvurderingDto(val type: DelvilkårType, val resultat: Vilkårsresultat)
