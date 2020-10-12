package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkar_vurdering")
data class VilkårVurdering(
        @Id
        val id: UUID = UUID.randomUUID(),
        @Column("behandling_id")
        val behandlingId: UUID,
        val resultat: VilkårResultat = VilkårResultat.IKKE_VURDERT,
        val type: VilkårType,
        val begrunnelse: String? = null,
        val unntak: String? = null,
        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
        val sporbar: Sporbar = Sporbar(),
        @Column("delvilkar")
        val delvilkårVurdering: DelvilkårVurderingWrapper
)

// Ingen støtte for å ha en liste direkt i entiteten, wrapper+converter virker
data class DelvilkårVurderingWrapper(val delvilkårVurderinger: List<DelvilkårVurdering>)

data class DelvilkårVurdering(val type: DelvilkårType,
                              val resultat: VilkårResultat = VilkårResultat.IKKE_VURDERT)

enum class DelvilkårType {
    DOKUMENTERT_FLYKTNINGSTATUS,
}

enum class VilkårResultat {
    JA,
    NEI,
    IKKE_VURDERT
}

//TODO Denne bør kanskje utvides til å inneholde en NARE-spesifikasjon
enum class VilkårType(val beskrivelse: String,
                      val delvilkår: List<DelvilkårType> = emptyList()) {

    OPPHOLDSTILLATELSE("Vises kun for ikke-nordiske statsborgere - Foreligger det oppholdstillatelse eller annen bekreftelse på gyldig opphold?"),
    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap",
                           listOf(DelvilkårType.DOKUMENTERT_FLYKTNINGSTATUS)),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold");

    companion object {

        fun hentInngangsvilkår(): List<VilkårType> = listOf(FORUTGÅENDE_MEDLEMSKAP, LOVLIG_OPPHOLD)
    }
}
