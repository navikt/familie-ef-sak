package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.nare.evaluations.Resultat
import java.util.*

data class VurderingDto(val id: UUID, //id på vurderingen i db
                        val behandlingId: UUID,
                        val resultat: Resultat,
                        val vilkårType: VilkårType,
                        val begrunnelse: String? = null,
                        val unntak: String? = null) //TODO usikker på hvordan vi skal representere unntak da det er forskjellig for ulike vilkårtyper

//TODO Denne bør kanskje flyttes og utvides til å inneholde en NARE-spesifikasjon
enum class VilkårType(val beskrivelse: String) {
    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap"),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold");
}