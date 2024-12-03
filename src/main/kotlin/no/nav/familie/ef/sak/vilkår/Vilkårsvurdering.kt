package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelVersjon
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.ef.StønadType.SKOLEPENGER
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * En vilkårsvurdering per type [VilkårType].
 * For noen typer så er det per [VilkårType] og [barnId], hvor man må vurdere vilkåret per barn til søkeren
 *
 * Hver vilkårsvurdering har delvilkår. Hvert delvilkår har vurderinger med svar, og kanskje begrunnelse.
 *
 * Husk at [opphavsvilkår] må tas stilling til når man kopierer denne
 */
@Table("vilkarsvurdering")
data class Vilkårsvurdering(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
    val type: VilkårType,
    val barnId: UUID? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    @Column("delvilkar")
    val delvilkårsvurdering: DelvilkårsvurderingWrapper,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "opphavsvilkaar_")
    val opphavsvilkår: Opphavsvilkår?,
    val kanGjenbrukes: Boolean = false,
) {
    init {
        require(resultat.erIkkeDelvilkårsresultat()) // Verdien AUTOMATISK_OPPFYLT er kun forbeholdt delvilkår
    }

    /**
     * Brukes når man skal gjenbruke denne vilkårsvurderingen i en annan vilkårsvurdering
     */
    fun opprettOpphavsvilkår(): Opphavsvilkår = opphavsvilkår ?: Opphavsvilkår(behandlingId, sporbar.endret.endretTid)

    fun gjeldendeDelvilkårsvurderinger(): List<Delvilkårsvurdering> =
        this.delvilkårsvurdering.delvilkårsvurderinger.filter {
            it.hovedregel.regelVersjon == RegelVersjon.GJELDENDE
        }
}

fun List<Vilkårsvurdering>.utledVurderinger(
    vilkårType: VilkårType,
    regelId: RegelId,
) = this
    .filter { it.type == vilkårType }
    .flatMap { it.delvilkårsvurdering.delvilkårsvurderinger }
    .flatMap { it.vurderinger }
    .filter { it.regelId == regelId }

/**
 * Inneholder informasjon fra hvilken behandling dette vilkår ble gjenrukt fra
 * Hvis man gjenbruker et vilkår som allerede er gjenbrukt fra en annen behandling,
 * så skal man peke til den opprinnelige behandlingen. Dvs
 * Behandling A
 * Behandling B gjenbruker fra behandling A
 * Behandling C gjenbruker fra B, men peker mot A sitt vilkår
 */
data class Opphavsvilkår(
    val behandlingId: UUID,
    val vurderingstidspunkt: LocalDateTime,
)

// Ingen støtte for å ha en liste direkt i entiteten, wrapper+converter virker
data class DelvilkårsvurderingWrapper(
    val delvilkårsvurderinger: List<Delvilkårsvurdering>,
)

data class Delvilkårsvurdering(
    val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
    val vurderinger: List<Vurdering>,
) {
    // regelId for første svaret er det samme som hovedregel
    val hovedregel = vurderinger.first().regelId
}

data class Vurdering(
    val regelId: RegelId,
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
)

fun List<Vurdering>.harSvar(svarId: SvarId) = this.any { it.svar == svarId }

val inngangsvilkår =
    listOf(
        VilkårType.FORUTGÅENDE_MEDLEMSKAP,
        VilkårType.LOVLIG_OPPHOLD,
        VilkårType.MOR_ELLER_FAR,
        VilkårType.SIVILSTAND,
        VilkårType.SAMLIV,
        VilkårType.ALENEOMSORG,
        VilkårType.NYTT_BARN_SAMME_PARTNER,
    )

enum class Vilkårsresultat(
    val beskrivelse: String,
) {
    OPPFYLT("Vilkåret er oppfylt når alle delvilkår er oppfylte"),
    AUTOMATISK_OPPFYLT("Delvilkår er oppfylt med automatisk beregning"),
    IKKE_OPPFYLT("Vilkåret er ikke oppfylt hvis alle delvilkår er oppfylt eller ikke oppfylt, men minimum 1 ikke oppfylt"),
    IKKE_AKTUELL("Hvis søknaden/pdl data inneholder noe som gjør att delvilkåret ikke må besvares"),
    IKKE_TATT_STILLING_TIL("Init state, eller att brukeren ikke svaret på hele delvilkåret"),
    SKAL_IKKE_VURDERES("Saksbehandleren kan sette att ett delvilkår ikke skal vurderes"),
    ;

    fun oppfyltEllerIkkeOppfylt() = this == OPPFYLT || this == IKKE_OPPFYLT

    fun erIkkeDelvilkårsresultat() = this != AUTOMATISK_OPPFYLT
}

enum class VilkårType(
    val beskrivelse: String,
    val gjelderStønader: List<StønadType>,
) {
    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap", listOf(OVERGANGSSTØNAD, BARNETILSYN, SKOLEPENGER)),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold", listOf(OVERGANGSSTØNAD, BARNETILSYN, SKOLEPENGER)),

    MOR_ELLER_FAR("§15-4 Mor eller far", listOf(OVERGANGSSTØNAD, BARNETILSYN, SKOLEPENGER)),

    SIVILSTAND("§15-4 Sivilstand", listOf(OVERGANGSSTØNAD, BARNETILSYN, SKOLEPENGER)),
    SAMLIV("§15-4 Samliv", listOf(OVERGANGSSTØNAD, BARNETILSYN, SKOLEPENGER)),
    ALENEOMSORG("§15-4 Aleneomsorg", listOf(OVERGANGSSTØNAD, BARNETILSYN, SKOLEPENGER)),
    NYTT_BARN_SAMME_PARTNER("§15-4 Nytt barn samme partner", listOf(OVERGANGSSTØNAD, BARNETILSYN, SKOLEPENGER)),
    SAGT_OPP_ELLER_REDUSERT("Sagt opp eller redusert stilling", listOf(OVERGANGSSTØNAD)),
    AKTIVITET("Aktivitet", listOf(OVERGANGSSTØNAD)),
    AKTIVITET_ARBEID("Aktivitet", listOf(BARNETILSYN)),
    TIDLIGERE_VEDTAKSPERIODER("Tidligere vedtaksperioder", listOf(OVERGANGSSTØNAD)),
    INNTEKT("§15-10 Inntekt", listOf(BARNETILSYN)),
    ALDER_PÅ_BARN("Alder på barn", listOf(BARNETILSYN)),
    DOKUMENTASJON_TILSYNSUTGIFTER("Dokumentasjon av tilsynsutgifter", listOf(BARNETILSYN)),
    RETT_TIL_OVERGANGSSTØNAD("Er vilkårene for rett til overgangsstønad oppfylt?", listOf(SKOLEPENGER)),
    DOKUMENTASJON_AV_UTDANNING("Dokumentasjon av utdanning", listOf(SKOLEPENGER)),
    ER_UTDANNING_HENSIKTSMESSIG("Er utdanning hensiktsmessig?", listOf(SKOLEPENGER)),

    ;

    fun gjelderFlereBarn(): Boolean = this == ALENEOMSORG || this == ALDER_PÅ_BARN

    fun erInngangsvilkår(): Boolean = inngangsvilkår.contains(this)

    companion object {
        fun hentVilkårForStønad(stønadstype: StønadType): List<VilkårType> =
            values().filter {
                it.gjelderStønader.contains(stønadstype)
            }
    }
}
