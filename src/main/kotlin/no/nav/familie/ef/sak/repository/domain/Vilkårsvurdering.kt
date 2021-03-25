package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

/**
 * En vilkårsvurdering per type [VilkårType].
 * For noen typer så er det per [VilkårType] og [barnId], hvor man må vurdere vilkåret per barn til søkeren
 *
 * Hver vilkårsvurdering har delvilkår. Hvert delvilkår har vurderinger med svar, og kanskje begrunnelse.
 *
 */
@Table("vilkarsvurdering")
data class Vilkårsvurdering(@Id
                            val id: UUID = UUID.randomUUID(),
                            val behandlingId: UUID,
                            val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                            val type: VilkårType,
                            val barnId: UUID? = null,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar(),
                            @Column("delvilkar")
                            val delvilkårsvurdering: DelvilkårsvurderingWrapper)

// Ingen støtte for å ha en liste direkt i entiteten, wrapper+converter virker
data class DelvilkårsvurderingWrapper(val delvilkårsvurderinger: List<Delvilkårsvurdering>)

data class Delvilkårsvurdering(val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                               val vurderinger: List<Vurdering>) {

    // regelId for første svaret er det samme som hovedregel
    val hovedregel = vurderinger.first().regelId

}

data class Vurdering(val regelId: RegelId,
                     val svar: SvarId? = null,
                     val begrunnelse: String? = null)

enum class Vilkårsresultat(val beskrivelse: String) {
    OPPFYLT("Vilkåret er oppfylt når alle delvilkår er oppfylte"),
    IKKE_OPPFYLT("Vilkåret er ikke oppfylt hvis alle delvilkår er oppfylt eller ikke oppfylt, men minimum 1 ikke oppfylt"),
    IKKE_AKTUELL("Hvis søknaden/pdl data inneholder noe som gjør att delvilkåret ikke må besvares"),
    IKKE_TATT_STILLING_TIL("Init state, eller att brukeren ikke svaret på hele delvilkåret"),
    SKAL_IKKE_VURDERES("Saksbehandleren kan sette att ett delvilkår ikke skal vurderes");

    fun oppfyltEllerIkkeOppfylt() = this == OPPFYLT || this == IKKE_OPPFYLT
}

enum class VilkårType(val beskrivelse: String) {

    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap"),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold"),

    MOR_ELLER_FAR("§15-4 Mor eller far"),

    SIVILSTAND("§15-4 Sivilstand"),
    SAMLIV("§15-4 Samliv"),
    ALENEOMSORG("§15-4 Aleneomsorg"),
    NYTT_BARN_SAMME_PARTNER("§15-4 Nytt barn samme partner"),
    SAGT_OPP_ELLER_REDUSERT("Sagt opp eller redusert stilling"),
    AKTIVITET("Aktivitet"),
    TIDLIGERE_VEDTAKSPERIODER("Tidligere vedtaksperioder");


    companion object {

        fun hentVilkår(): List<VilkårType> = values().toList()
    }
}
