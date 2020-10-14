package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkarsvurdering")
data class Vilkårsvurdering(
        @Id
        val id: UUID = UUID.randomUUID(),
        @Column("behandling_id")
        val behandlingId: UUID,
        val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_VURDERT,
        val type: Vilkårstype,
        val begrunnelse: String? = null,
        val unntak: String? = null,
        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
        val sporbar: Sporbar = Sporbar(),
        @Column("delvilkar")
        val delvilkårsvurdering: DelvilkårsvurderingWrapper
)

// Ingen støtte for å ha en liste direkt i entiteten, wrapper+converter virker
data class DelvilkårsvurderingWrapper(val delvilkårsvurderinger: List<Delvilkårsvurdering>)

data class Delvilkårsvurdering(val type: DelvilkårsType,
                               val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_VURDERT)

enum class DelvilkårsType {
    TRE_ÅRS_MEDLEMSKAP,
    DOKUMENTERT_FLYKTNINGSTATUS,
    BOR_OG_OPPHOLDER_SEG_I_NORGE,
}

enum class Vilkårsresultat {
    JA,
    NEI,
    IKKE_VURDERT
}

//TODO Denne bør kanskje utvides til å inneholde en NARE-spesifikasjon
enum class Vilkårstype(val beskrivelse: String,
                       val delvilkår: List<DelvilkårsType> = emptyList()) {

    OPPHOLDSTILLATELSE("Vises kun for ikke-nordiske statsborgere - Foreligger det oppholdstillatelse eller annen bekreftelse på gyldig opphold?"),
    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap",
                           listOf(DelvilkårsType.TRE_ÅRS_MEDLEMSKAP, DelvilkårsType.DOKUMENTERT_FLYKTNINGSTATUS)),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold", listOf(DelvilkårsType.BOR_OG_OPPHOLDER_SEG_I_NORGE));

    companion object {

        fun hentInngangsvilkår(): List<Vilkårstype> = listOf(FORUTGÅENDE_MEDLEMSKAP, LOVLIG_OPPHOLD)
    }
}
