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
        val sporbar: Sporbar = Sporbar()
)

enum class VilkårResultat {
    JA,
    NEI,
    IKKE_VURDERT
}

//TODO Denne bør kanskje utvides til å inneholde en NARE-spesifikasjon
enum class VilkårType(val beskrivelse: String) {

    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap"),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold");

    companion object {

        fun hentInngangsvilkår(): List<VilkårType> = listOf(FORUTGÅENDE_MEDLEMSKAP, LOVLIG_OPPHOLD)
    }
}
