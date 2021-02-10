package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkarsvurdering")
data class Vilkårsvurdering(@Id
                            val id: UUID = UUID.randomUUID(),
                            val behandlingId: UUID,
                            val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_VURDERT,
                            val type: VilkårType,
                            val begrunnelse: String? = null,
                            val unntak: String? = null,
                            val barnId: UUID? = null,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar(),
                            @Column("delvilkar")
                            val delvilkårsvurdering: DelvilkårsvurderingWrapper)

// Ingen støtte for å ha en liste direkt i entiteten, wrapper+converter virker
data class DelvilkårsvurderingWrapper(val delvilkårsvurderinger: List<Delvilkårsvurdering>)

data class Delvilkårsvurdering(val type: DelvilkårType,
                               val resultat: Vilkårsresultat = Vilkårsresultat.IKKE_VURDERT,
                               val årsak: DelvilkårÅrsak? = null,
                               val begrunnelse: String? = null)

data class DelvilkårMetadata(val sivilstandstype: Sivilstandstype)

enum class DelvilkårType {
    FEM_ÅRS_MEDLEMSKAP,
    DOKUMENTERT_FLYKTNINGSTATUS,
    BOR_OG_OPPHOLDER_SEG_I_NORGE,
    DOKUMENTERT_EKTESKAP,
    DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
    SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
    SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
    KRAV_SIVILSTAND,
    HAR_FLYTTET_FRA_HVERANDRE,
    LEVER_IKKE_MED_ANNEN_FORELDER,
    LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD,
    SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
    NÆRE_BOFORHOLD,
    MER_AV_DAGLIG_OMSORG,
}

enum class DelvilkårÅrsak {
    SAMME_HUS_OG_FÆRRE_ENN_4_BOENHETER,
    SAMME_HUS_OG_FLERE_ENN_4_BOENHETER_MEN_VURDERT_NÆRT,
    SELVSTENDIGE_BOLIGER_SAMME_TOMT,
    SELVSTENDIGE_BOLIGER_SAMME_GÅRDSTUN,
    NÆRMESTE_BOLIG_ELLER_REKKEHUS_I_SAMMEGATE,
    TILSTØTENDE_BOLIGER_ELLER_REKKEHUS_I_SAMMEGATE
}

enum class Vilkårsresultat {
    JA,
    NEI,
    IKKE_VURDERT,
    IKKE_AKTUELL
}

//TODO Denne bør kanskje utvides til å inneholde en NARE-spesifikasjon
enum class VilkårType(val beskrivelse: String,
                      val delvilkår: List<DelvilkårType> = emptyList()) {

    OPPHOLDSTILLATELSE("Vises kun for ikke-nordiske statsborgere - " +
                       "Foreligger det oppholdstillatelse eller annen bekreftelse på gyldig opphold?"),
    FORUTGÅENDE_MEDLEMSKAP("§15-2 Forutgående medlemskap",
                           listOf(DelvilkårType.FEM_ÅRS_MEDLEMSKAP)),
    LOVLIG_OPPHOLD("§15-3 Lovlig opphold", listOf(DelvilkårType.BOR_OG_OPPHOLDER_SEG_I_NORGE)),

    SIVILSTAND("§15-4 Sivilstand",
               listOf(
                       DelvilkårType.DOKUMENTERT_EKTESKAP,
                       DelvilkårType.DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                       DelvilkårType.SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                       DelvilkårType.SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                       DelvilkårType.KRAV_SIVILSTAND,
               )),
    SAMLIV("§15-4 Samliv",
           listOf(DelvilkårType.HAR_FLYTTET_FRA_HVERANDRE,
                  DelvilkårType.LEVER_IKKE_MED_ANNEN_FORELDER,
                  DelvilkårType.LEVER_IKKE_I_EKTESKAPLIGNENDE_FORHOLD)),
    ALENEOMSORG("§15-4 Aleneomsorg",
                listOf(DelvilkårType.SKRIFTLIG_AVTALE_OM_DELT_BOSTED,
                       DelvilkårType.NÆRE_BOFORHOLD,
                       DelvilkårType.MER_AV_DAGLIG_OMSORG));


    companion object {

        fun hentInngangsvilkår(): List<VilkårType> = listOf(FORUTGÅENDE_MEDLEMSKAP,
                                                            LOVLIG_OPPHOLD,
                                                            SIVILSTAND,
                                                            SAMLIV,
                                                            ALENEOMSORG)
    }
}
