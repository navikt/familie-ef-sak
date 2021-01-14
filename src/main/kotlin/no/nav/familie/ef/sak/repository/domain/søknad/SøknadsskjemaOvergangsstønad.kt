package no.nav.familie.ef.sak.repository.domain.søknad

import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

@Table("soknadsskjema")
data class SøknadsskjemaOvergangsstønad(@Id
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
                                        @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "situasjon_")
                                        val situasjon: Situasjon,
                                        @Column("soker_fra")
                                        val søkerFra: YearMonth?,
                                        @Column("soker_fra_bestemt_maned")
                                        val søkerFraBestemtMåned: Boolean) : ISøknadsskjema

data class Situasjon(@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "gjelder_dette_deg_")
                     val gjelderDetteDeg: EnumTekstverdiMedSvarId,
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
                     @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sagt_opp_eller_redusert_stilling_")
                     val sagtOppEllerRedusertStilling: EnumTekstverdiMedSvarId? = null,
                     @Column("oppsigelse_reduksjon_arsak")
                     val oppsigelseReduksjonÅrsak: String? = null,
                     val oppsigelseReduksjonTidspunkt: LocalDate? = null,
                     val reduksjonAvArbeidsforholdDokumentasjon: Dokumentasjon? = null,
                     val oppsigelseDokumentasjon: Dokumentasjon? = null)
