package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Table("soknadsskjema")
data class SøknadsskjemaSkolepenger(
    @Id
    override val id: UUID = UUID.randomUUID(),
    override val type: SøknadType,
    @Column("fodselsnummer")
    override val fødselsnummer: String,
    override val navn: String,
    override val datoMottatt: LocalDateTime,
    @Column("dato_pabegynt_soknad")
    override val datoPåbegyntSøknad: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sivilstand_")
    val sivilstand: Sivilstand,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "medlemskap_")
    val medlemskap: Medlemskap,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "bosituasjon_")
    val bosituasjon: Bosituasjon,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sivilstandsplaner_")
    val sivilstandsplaner: Sivilstandsplaner? = null,
    @MappedCollection(idColumn = "soknadsskjema_id")
    override val barn: Set<SøknadBarn>,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "aktivitet_under_utdanning_")
    val utdanning: UnderUtdanning,
    val utdanningsutgifter: Dokumentasjon? = null,
    @MappedCollection(idColumn = "utdanning_dokumentasjon")
    val utdanningDokumentasjon: Dokumentasjon? = null,
    @MappedCollection(idColumn = "soknadsskjema_id")
    val tidligereUtdanninger: Set<TidligereUtdanning> = emptySet(),
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY, prefix = "adresseopplysninger_")
    val adresseopplysninger: Adresseopplysninger?,
) : ISøknadsskjema
