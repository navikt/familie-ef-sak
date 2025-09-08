package no.nav.familie.ef.sak.andreytelser

import java.time.LocalDate

data class ArbeidsavklaringspengerResponse(
    val barnMedStonad: Int,
    val barnetillegg: Int,
    val beregningsgrunnlag: Int,
    val dagsats: Int,
    val dagsatsEtterUføreReduksjon: Int,
    val kildesystem: String,
    val opphorsAarsak: String,
    val periode: Periode,
    val rettighetsType: String,
    val saksnummer: String,
    val samordningsId: String,
    val status: String,
    val vedtakId: String,
    val vedtaksTypeKode: String,
    val vedtaksTypeNavn: String,
    val vedtaksdato: LocalDate,
)

data class Periode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
)