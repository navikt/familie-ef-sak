package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

interface IBarn {
    val fødselsnummer: String?
    val annenForelder: IAnnenForelder?
}

@Table("soknad_barn")
data class SøknadBarn(
    @Id
    val id: UUID = UUID.randomUUID(),
    val navn: String? = null,
    @Column("fodselsnummer")
    override val fødselsnummer: String? = null,
    val harSkalHaSammeAdresse: Boolean,
    @Column("skal_bo_hos_soker")
    val skalBoHosSøker: String? = null,
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
    @Column("barnepass_arsak_barnepass")
    val årsakBarnepass: String? = null,
    @MappedCollection(idColumn = "barn_id")
    val barnepassordninger: Set<Barnepassordning> = emptySet(),
    val lagtTilManuelt: Boolean,
) : IBarn

interface IAnnenForelder {
    val person: PersonMinimum?
}

data class AnnenForelder(
    val ikkeOppgittAnnenForelderBegrunnelse: String? = null,
    val bosattNorge: Boolean? = null,
    val land: String? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    override val person: PersonMinimum? = null,
    val erKopiertFraAnnetBarn: Boolean? = null,
) : IAnnenForelder

@Table("soknad_barnepassordning")
data class Barnepassordning(
    val hvaSlagsBarnepassordning: String,
    val navn: String,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    val datoperiode: Datoperiode,
    @Column("belop")
    val beløp: Int,
)

data class Samvær(
    @Column("sporsmal_avtale_om_delt_bosted")
    val spørsmålAvtaleOmDeltBosted: Boolean? = null,
    val avtaleOmDeltBosted: Dokumentasjon? = null,
    @Column("skal_annen_forelder_ha_samver")
    val skalAnnenForelderHaSamvær: String? = null,
    @Column("har_dere_skriftlig_avtale_om_samver")
    val harDereSkriftligAvtaleOmSamvær: String? = null,
    @Column("samversavtale")
    val samværsavtale: Dokumentasjon? = null,
    @Column("barn_skal_bo_hos_soker_annen_forelder_samarbeider_ikke")
    val skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke: Dokumentasjon? = null,
    @Column("hvordan_praktiseres_samveret")
    val hvordanPraktiseresSamværet: String? = null,
    val borAnnenForelderISammeHus: String? = null,
    val borAnnenForelderISammeHusBeskrivelse: String? = null,
    val harDereTidligereBoddSammen: Boolean? = null,
    @Column("nar_flyttet_dere_fra_hverandre")
    val nårFlyttetDereFraHverandre: LocalDate? = null,
    @Column("erklering_om_samlivsbrudd")
    val erklæringOmSamlivsbrudd: Dokumentasjon? = null,
    val hvorMyeErDuSammenMedAnnenForelder: String? = null,
    @Column("beskriv_samver_uten_barn")
    val beskrivSamværUtenBarn: String? = null,
)
