package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.domain.Vedtaksperiode

enum class ResultatType {
    INNVILGE,
    AVSLÅ,
    HENLEGGE
}

data class VedtakDto(val resultatType: ResultatType,
                     val periodeBegrunnelse: String,
                     val inntektBegrunnelse: String,
                     val perioder: List<Vedtaksperiode> = emptyList(),
                     val inntekter: List<Inntektsperiode> = emptyList())
