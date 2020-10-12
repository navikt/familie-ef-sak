package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.DelvilkårType
import no.nav.familie.ef.sak.repository.domain.VilkårResultat
import no.nav.familie.ef.sak.repository.domain.VilkårType
import java.time.LocalDateTime
import java.util.*

data class VurderingDto(val id: UUID,
                        val behandlingId: UUID,
                        val resultat: VilkårResultat,
                        val vilkårType: VilkårType,
                        val begrunnelse: String? = null,
                        val unntak: String? = null,
                        val endretAv: String,
                        val endretTid: LocalDateTime,
                        val delvurderinger: List<DelvurderingDto> = emptyList())

data class DelvurderingDto(val type: DelvilkårType, val resultat: VilkårResultat)
