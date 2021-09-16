package no.nav.familie.ef.sak.no.nav.familie.ef.sak.regler.evalutation

import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VurderingDto

fun delvilkårsvurderingDto(vararg vurderinger: VurderingDto) =
        DelvilkårsvurderingDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())