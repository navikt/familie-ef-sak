package no.nav.familie.ef.sak.repository.domain.søknad

import no.nav.familie.ef.sak.repository.domain.SøknadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("soknadsskjema")
data class SøknadsskjemaSkolepenger(@Id
                                    override val id: UUID = UUID.randomUUID(),
                                    override val type: SøknadType,
                                    @Column("fodselsnummer")
                                    override val fødselsnummer: String,
                                    override val navn: String,
                                    override val telefonnummer: String?,
                                    override val datoMottatt: LocalDateTime,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "medlemskap_")
                                    val medlemskap: Medlemskap,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "bosituasjon_")
                                    val bosituasjon: Bosituasjon,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sivilstandsplaner_")
                                    val sivilstandsplaner: Sivilstandsplaner? = null,
                                    @MappedCollection(idColumn = "soknadsskjema_id")
                                    override val barn: Set<Barn>,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "aktivitet_under_utdanning_")
                                    val utdanning: UnderUtdanning,
                                    val utdanningsutgifter: Dokumentasjon? = null) : ISøknadsskjema

