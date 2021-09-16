package no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad

import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate

data class Aktivitet(val hvordanErArbeidssituasjonen: Arbeidssituasjon = Arbeidssituasjon(emptyList()),
                     @MappedCollection(idColumn = "soknadsskjema_id")
                     val arbeidsforhold: Set<Arbeidsgiver>? = emptySet(),
                     @MappedCollection(idColumn = "soknadsskjema_id")
                     val firmaer: Set<Selvstendig>? = emptySet(),
                     @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "virksomhet_")
                     val virksomhet: Virksomhet? = null,
                     @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "arbeidssoker_")
                     val arbeidssøker: Arbeidssøker? = null,
                     @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "under_utdanning_")
                     val underUtdanning: UnderUtdanning? = null,
                     @MappedCollection(idColumn = "soknadsskjema_id")
                     val aksjeselskap: Set<Aksjeselskap>? = emptySet(),
                     val erIArbeid: String? = null,
                     val erIArbeidDokumentasjon: Dokumentasjon? = null,
                     @MappedCollection(idColumn = "soknadsskjema_id")
                     val tidligereUtdanninger: Set<TidligereUtdanning> = emptySet()
)

data class Arbeidsgiver(val arbeidsgivernavn: String,
                        val arbeidsmengde: Int? = null,
                        val fastEllerMidlertidig: String? = null,
                        val harSluttdato: Boolean?,
                        val sluttdato: LocalDate? = null)

data class Selvstendig(val firmanavn: String,
                       val organisasjonsnummer: String,
                       val etableringsdato: LocalDate,
                       val arbeidsmengde: Int? = null,
                       val hvordanSerArbeidsukenUt: String)

data class Virksomhet(val virksomhetsbeskrivelse: String,
                      val dokumentasjon: Dokumentasjon? = null)

data class Aksjeselskap(val navn: String,
                        val arbeidsmengde: Int? = null)

data class Arbeidssituasjon(val verdier: List<String>)