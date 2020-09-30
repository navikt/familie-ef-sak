package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.VilkårResultat
import no.nav.familie.ef.sak.repository.domain.VilkårType
import java.util.*

data class VurderingDto(val id: UUID, //id på vurderingen i db
                        val behandlingId: UUID,
                        val resultat: VilkårResultat,
                        val vilkårType: VilkårType,
                        val begrunnelse: String? = null,
                        val unntak: String? = null) //TODO usikker på hvordan vi skal representere unntak da det er forskjellig for ulike vilkårtyper
