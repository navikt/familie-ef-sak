package no.nav.familie.ef.sak.vilkår.regler.evalutation

import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto

fun delvilkårsvurderingDto(vararg vurderinger: VurderingDto) = DelvilkårsvurderingDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())
