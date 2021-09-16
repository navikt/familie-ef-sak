package no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad

import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

@Table("soknadsskjema")
data class SøknadsskjemaBarnetilsyn(@Id
                                    override val id: UUID = UUID.randomUUID(),
                                    override val type: SøknadType,
                                    @Column("fodselsnummer")
                                    override val fødselsnummer: String,
                                    override val navn: String,
                                    override val telefonnummer: String?,
                                    override val datoMottatt: LocalDateTime,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sivilstand_")
                                    val sivilstand: Sivilstand,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "medlemskap_")
                                    val medlemskap: Medlemskap,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "bosituasjon_")
                                    val bosituasjon: Bosituasjon,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sivilstandsplaner_")
                                    val sivilstandsplaner: Sivilstandsplaner? = null,
                                    @MappedCollection(idColumn = "soknadsskjema_id")
                                    override val barn: Set<Barn>,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "aktivitet_")
                                    val aktivitet: Aktivitet,
                                    @Column("soker_fra")
                                    val søkerFra: YearMonth?,
                                    @Column("soker_fra_bestemt_maned")
                                    val søkerFraBestemtMåned: Boolean,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "dokumentasjon_")
                                    val dokumentasjon: BarnetilsynDokumentasjon) : ISøknadsskjema

data class BarnetilsynDokumentasjon(val barnepassordningFaktura: Dokumentasjon? = null,
                                    val avtaleBarnepasser: Dokumentasjon? = null,
                                    val arbeidstid: Dokumentasjon? = null,
                                    val roterendeArbeidstid: Dokumentasjon? = null,
                                    val spesielleBehov: Dokumentasjon? = null)
