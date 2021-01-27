package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.søknad.Bosituasjon

data class InngangsvilkårDto(val vurderinger: List<VilkårsvurderingDto>,
                             val grunnlag: InngangsvilkårGrunnlagDto)

data class InngangsvilkårGrunnlagDto(val medlemskap: MedlemskapDto,
                                     val sivilstand: SivilstandInngangsvilkårDto,
                                     val bosituasjon: BosituasjonDto)
