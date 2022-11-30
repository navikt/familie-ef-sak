package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Table("soknadsskjema")
data class SøknadsskjemaOvergangsstønad(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val type: SøknadType,
    @Column("fodselsnummer")
    override val fødselsnummer: String,
    override val navn: String,
    override val datoMottatt: LocalDateTime,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sivilstand_")
    val sivilstand: Sivilstand,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "medlemskap_")
    val medlemskap: Medlemskap,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "bosituasjon_")
    val bosituasjon: Bosituasjon,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY, prefix = "sivilstandsplaner_")
    val sivilstandsplaner: Sivilstandsplaner,
    @MappedCollection(idColumn = "soknadsskjema_id")
    override val barn: Set<SøknadBarn>,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "aktivitet_")
    val aktivitet: Aktivitet,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "situasjon_")
    val situasjon: Situasjon,
    @Column("soker_fra")
    val søkerFra: YearMonth?,
    @Column("soker_fra_bestemt_maned")
    val søkerFraBestemtMåned: Boolean,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY, prefix = "adresseopplysninger_")
    val adresseopplysninger: Adresseopplysninger
) : ISøknadsskjema

data class Situasjon(
    val gjelderDetteDeg: GjelderDeg = GjelderDeg(emptyList()),
    val sykdom: Dokumentasjon? = null,
    val barnsSykdom: Dokumentasjon? = null,
    val manglendeBarnepass: Dokumentasjon? = null,
    @Column("barn_med_serlige_behov")
    val barnMedSærligeBehov: Dokumentasjon? = null,
    val arbeidskontrakt: Dokumentasjon? = null,
    @Column("lerlingkontrakt")
    val lærlingkontrakt: Dokumentasjon? = null,
    val oppstartNyJobb: LocalDate? = null,
    val utdanningstilbud: Dokumentasjon? = null,
    val oppstartUtdanning: LocalDate? = null,
    val sagtOppEllerRedusertStilling: String? = null,
    @Column("oppsigelse_reduksjon_arsak")
    val oppsigelseReduksjonÅrsak: String? = null,
    val oppsigelseReduksjonTidspunkt: LocalDate? = null,
    val reduksjonAvArbeidsforholdDokumentasjon: Dokumentasjon? = null,
    val oppsigelseDokumentasjon: Dokumentasjon? = null
)

data class GjelderDeg(val verdier: List<String>)
