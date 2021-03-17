package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

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

//Kan vi bestemme att vi skal vurdere alle delvilkår eller ikke, og att vi ikke trenger resultat på delviljkår
data class Delvilkårsvurdering(val type: RegelId,
                               val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                               val svar: List<VilkårSvar>)

data class VilkårSvar(val regelId: RegelId,
                      val svar: SvarId?,
                      val begrunnelse: String?)

data class DelvilkårMetadata(val sivilstandstype: Sivilstandstype)

enum class Vilkårsresultat {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_TATT_STILLING_TIL,
    SKAL_IKKE_VURDERES
}

enum class VilkårType(val beskrivelse: String) {

    //TODO burde denne slettes?
    OPPHOLDSTILLATELSE("Vises kun for ikke-nordiske statsborgere - " +
                       "Foreligger det oppholdstillatelse eller annen bekreftelse på gyldig opphold?"),
    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap"),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold"),

    MOR_ELLER_FAR("§15-4 Mor eller far"),

    SIVILSTAND("§15-4 Sivilstand"),
    SAMLIV("§15-4 Samliv"),
    ALENEOMSORG("§15-4 Aleneomsorg"),
    NYTT_BARN_SAMME_PARTNER("§15-4 Nytt barn samme partner");

    companion object {

        fun hentVilkår(): List<VilkårType> = listOf(FORUTGÅENDE_MEDLEMSKAP,
                                                    LOVLIG_OPPHOLD,
                                                    MOR_ELLER_FAR,
                                                    SIVILSTAND,
                                                    SAMLIV,
                                                    ALENEOMSORG,
                                                    NYTT_BARN_SAMME_PARTNER)
    }
}
