package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
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

enum class VilkårType(val beskrivelse: String, val gjelderStønader: List<StønadType>) {

    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap", listOf(OVERGANGSSTØNAD, BARNETILSYN)),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold", listOf(OVERGANGSSTØNAD, BARNETILSYN)),

    MOR_ELLER_FAR("§15-4 Mor eller far", listOf(OVERGANGSSTØNAD, BARNETILSYN)),

    SIVILSTAND("§15-4 Sivilstand", listOf(OVERGANGSSTØNAD, BARNETILSYN)),
    SAMLIV("§15-4 Samliv", listOf(OVERGANGSSTØNAD, BARNETILSYN)),
    ALENEOMSORG("§15-4 Aleneomsorg", listOf(OVERGANGSSTØNAD, BARNETILSYN)),
    NYTT_BARN_SAMME_PARTNER("§15-4 Nytt barn samme partner", listOf(OVERGANGSSTØNAD, BARNETILSYN)),
    SAGT_OPP_ELLER_REDUSERT("Sagt opp eller redusert stilling", listOf(OVERGANGSSTØNAD)),
    AKTIVITET("Aktivitet", listOf(OVERGANGSSTØNAD)),
    AKTIVITET_ARBEID("Aktivitet", listOf(BARNETILSYN)),
    TIDLIGERE_VEDTAKSPERIODER("Tidligere vedtaksperioder", listOf(OVERGANGSSTØNAD)),
    INNTEKT("§15-10 Inntekt", listOf(BARNETILSYN)),
    ALDER_PÅ_BARN("Alder på barn", listOf(BARNETILSYN)),
    ;

    fun gjelderFlereBarn(): Boolean = this == ALENEOMSORG || this == ALDER_PÅ_BARN


    companion object {

        fun hentVilkårForStønad(stønadstype: StønadType): List<VilkårType> = values().filter {
            it.gjelderStønader.contains(stønadstype)
        }


    }
}
