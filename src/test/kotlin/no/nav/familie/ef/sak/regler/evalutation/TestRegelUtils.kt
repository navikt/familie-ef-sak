package no.nav.familie.ef.sak.no.nav.familie.ef.sak.regler.evalutation

import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat

fun delvilkårsvurderingDto(vararg vurderinger: VurderingDto) =
        DelvilkårsvurderingDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())