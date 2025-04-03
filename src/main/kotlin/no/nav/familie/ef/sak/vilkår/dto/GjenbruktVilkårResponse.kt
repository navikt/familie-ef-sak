package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilDto
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering

data class GjenbruktVilkårResponse(
    val vilkårsvurdering: Vilkårsvurdering,
    val samværsavtaler: List<Samværsavtale>,
)

data class GjenbruktVilkårResponseDto(
    val vilkårsvurdering: VilkårsvurderingDto,
    val samværsavtaler: List<SamværsavtaleDto>,
)

fun GjenbruktVilkårResponse.tilDto(): GjenbruktVilkårResponseDto =
    GjenbruktVilkårResponseDto(
        vilkårsvurdering = this.vilkårsvurdering.tilDto(),
        samværsavtaler = this.samværsavtaler.tilDto(),
    )
