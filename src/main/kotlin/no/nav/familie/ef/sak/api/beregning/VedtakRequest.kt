package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.domain.Vedtaksperiode
import java.math.BigDecimal
import java.time.LocalDate

enum class ResultatType {
    INNVILGE,
    AVSLÅ,
    HENLEGGE,
    ANNULLERE
}

data class VedtakRequest(val resultatType: ResultatType,
                         val periodeBegrunnelse: String,
                         val inntektBegrunnelse: String,
                         val perioder: List<Vedtaksperiode> = emptyList(),
                         val inntekter: List<Inntektsperiode> = emptyList())
