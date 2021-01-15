package no.nav.familie.ef.sak.repository.domain.søknad

import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.*

interface IBarn {

    val fødselsnummer: String?
    val annenForelder: IAnnenForelder?
}

data class Barn(@Id
                val id: UUID = UUID.randomUUID(),
                val navn: String? = null,
                @Column("fodselsnummer")
                override val fødselsnummer: String? = null,
                val harSkalHaSammeAdresse: Boolean,
                @Column("ikke_registrert_pa_sokers_adresse_beskrivelse")
                val ikkeRegistrertPåSøkersAdresseBeskrivelse: String?,
                @Column("er_barnet_fodt")
                val erBarnetFødt: Boolean,
                @Column("fodsel_termindato")
                val fødselTermindato: LocalDate? = null,
                val terminbekreftelse: Dokumentasjon? = null,
                @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "annen_forelder_")
                override val annenForelder: AnnenForelder? = null,
                @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "samver_")
                val samvær: Samvær? = null,
                val skalHaBarnepass: Boolean? = null,
                @Column("serlige_tilsynsbehov")
                val særligeTilsynsbehov: String? = null,
                @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "barnepass_")
                val barnepass: Barnepass? = null) : IBarn

interface IAnnenForelder {

    val person: PersonMinimum?
}

data class AnnenForelder(@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "ikke_oppgitt_annen_forelder_begrunnelse_")
                         val ikkeOppgittAnnenForelderBegrunnelse: EnumTekstverdiMedSvarId? = null,
                         val bosattNorge: Boolean? = null,
                         val land: String? = null,
                         @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
                         override val person: PersonMinimum? = null) : IAnnenForelder

data class Barnepass(@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "arsak_barnepass_")
                     val årsakBarnepass: EnumTekstverdiMedSvarId? = null,
                     @MappedCollection(idColumn = "barn_id")
                     val barnepassordninger: Set<Barnepassordning>?)

data class Barnepassordning(@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "hva_slags_barnepassordning_")
                            val hvaSlagsBarnepassordning: EnumTekstverdiMedSvarId,
                            val navn: String,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
                            val datoperiode: Datoperiode? = null,
                            @Column("belop")
                            val beløp: Int)

data class Samvær(@Column("sporsmal_avtale_om_delt_bosted")
                  val spørsmålAvtaleOmDeltBosted: Boolean? = null,
                  val avtaleOmDeltBosted: Dokumentasjon? = null,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "skal_annen_forelder_ha_samver_")
                  val skalAnnenForelderHaSamvær: EnumTekstverdiMedSvarId? = null,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "har_dere_skriftlig_avtale_om_samver_")
                  val harDereSkriftligAvtaleOmSamvær: EnumTekstverdiMedSvarId? = null,
                  @Column("samversavtale")
                  val samværsavtale: Dokumentasjon? = null,
                  @Column("barn_skal_bo_hos_soker_annen_forelder_samarbeider_ikke")
                  val skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke: Dokumentasjon? = null,
                  @Column("hvordan_praktiseres_samveret")
                  val hvordanPraktiseresSamværet: String? = null,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "bor_annen_forelder_i_samme_hus_")
                  val borAnnenForelderISammeHus: EnumTekstverdiMedSvarId? = null,
                  val borAnnenForelderISammeHusBeskrivelse: String? = null,
                  val harDereTidligereBoddSammen: Boolean? = null,
                  @Column("nar_flyttet_dere_fra_hverandre")
                  val nårFlyttetDereFraHverandre: LocalDate? = null,
                  @Column("erklering_om_samlivsbrudd")
                  val erklæringOmSamlivsbrudd: Dokumentasjon? = null,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "hvor_mye_er_du_sammen_med_annen_forelder_")
                  val hvorMyeErDuSammenMedAnnenForelder: EnumTekstverdiMedSvarId? = null,
                  @Column("beskriv_samver_uten_barn")
                  val beskrivSamværUtenBarn: String? = null)

