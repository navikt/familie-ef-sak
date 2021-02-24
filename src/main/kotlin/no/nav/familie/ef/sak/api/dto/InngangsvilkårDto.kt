package no.nav.familie.ef.sak.api.dto



data class InngangsvilkårDto(val vurderinger: List<VilkårsvurderingDto>,
                             val grunnlag: InngangsvilkårGrunnlagDto)

data class InngangsvilkårGrunnlagDto(val medlemskap: MedlemskapDto,
                                     val sivilstand: SivilstandInngangsvilkårDto,
                                     val bosituasjon: BosituasjonDto,
                                     val barnMedSamvær: List<BarnMedSamværDto>,
                                     val sivilstandsplaner: SivilstandsplanerDto
                                    )
