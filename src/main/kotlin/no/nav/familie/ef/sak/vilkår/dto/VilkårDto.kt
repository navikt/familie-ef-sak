package no.nav.familie.ef.sak.vilkår.dto

import java.time.LocalDateTime


data class VilkårDto(val vurderinger: List<VilkårsvurderingDto>,
                     val grunnlag: VilkårGrunnlagDto)

data class VilkårGrunnlagDto(val tidligereVedtaksperioder: TidligereVedtaksperioderDto,
                             val medlemskap: MedlemskapDto,
                             val sivilstand: SivilstandInngangsvilkårDto,
                             val bosituasjon: BosituasjonDto,
                             val barnMedSamvær: List<BarnMedSamværDto>,
                             val sivilstandsplaner: SivilstandsplanerDto,
                             val aktivitet: AktivitetDto,
                             val sagtOppEllerRedusertStilling: SagtOppEllerRedusertStillingDto,
                             val lagtTilEtterFerdigstilling: Boolean,
                             val registeropplysningerOpprettetTid: LocalDateTime)
