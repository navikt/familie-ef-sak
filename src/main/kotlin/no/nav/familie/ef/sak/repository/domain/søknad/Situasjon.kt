package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Situasjon(val gjelderDetteDeg: List<String>,
                     val sykdom: Dokumentasjon? = null,
                     val barnsSykdom: Dokumentasjon? = null,
                     val manglendeBarnepass: Dokumentasjon? = null,
                     val barnMedSærligeBehov: Dokumentasjon? = null,
                     val arbeidskontrakt: Dokumentasjon? = null,
                     val lærlingkontrakt: Dokumentasjon? = null,
                     val oppstartNyJobb: LocalDate? = null,
                     val utdanningstilbud: Dokumentasjon? = null,
                     val oppstartUtdanning: LocalDate? = null,
                     val sagtOppEllerRedusertStilling: String? = null,
                     val oppsigelseReduksjonÅrsak: String? = null,
                     val oppsigelseReduksjonTidspunkt: LocalDate? = null,
                     val reduksjonAvArbeidsforholdDokumentasjon: Dokumentasjon? = null,
                     val oppsigelseDokumentasjon: Dokumentasjon? = null)
