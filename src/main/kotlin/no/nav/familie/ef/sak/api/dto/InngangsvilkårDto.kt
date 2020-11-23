package no.nav.familie.ef.sak.api.dto

data class InngangsvilkårDto(val medlemskap: MedlemskapDto,
                             val sivilstand: SivilstandInngangsvilkårDto,
                             val vurderinger: List<VilkårsvurderingDto>)
