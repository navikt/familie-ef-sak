package no.nav.familie.ef.sak.api.dto


data class VilkårDto(val vurderinger: List<VilkårsvurderingDto>,
                     val grunnlag: VilkårGrunnlagDto)

data class VilkårGrunnlagDto(val medlemskap: MedlemskapDto,
                             val sivilstand: SivilstandInngangsvilkårDto,
                             val bosituasjon: BosituasjonDto,
                             val barnMedSamvær: List<BarnMedSamværDto>,
                             val sivilstandsplaner: SivilstandsplanerDto)
