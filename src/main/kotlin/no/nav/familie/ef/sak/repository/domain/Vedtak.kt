package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.beregning.Inntektsperiode
import no.nav.familie.ef.sak.api.beregning.ResultatType
import org.springframework.data.annotation.Id
import java.time.LocalDate
import java.util.*


data class Vedtak(@Id
                  val behandlingId: UUID,
                  val resultatType: ResultatType,
                  val periodeBegrunnelse: String,
                  val inntektBegrunnelse: String,
                  val perioder: PeriodeWrapper,
                  val inntekter: InntektWrapper)

data class Vedtaksperiode(
        val datoFra: LocalDate,
        val datoTil: LocalDate,
        val aktivitet: String,
        val periodeType: String)

data class PeriodeWrapper(val perioder: List<Vedtaksperiode>)
data class InntektWrapper(val inntekter: List<Inntektsperiode>)