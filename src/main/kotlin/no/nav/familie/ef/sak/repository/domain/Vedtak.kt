package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.beregning.Inntektsperiode
import no.nav.familie.ef.sak.api.beregning.ResultatType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.*


data class Vedtak(@Id
                  val behandlingId: UUID,
                  val resultatType: ResultatType,
                  val periodeBegrunnelse: String? = null,
                  val inntektBegrunnelse: String? = null,
                  @Column("avsla_begrunnelse")
                  val avslåBegrunnelse: String? = null,
                  val perioder: PeriodeWrapper? = null,
                  val inntekter: InntektWrapper? = null)

data class Vedtaksperiode(
        val datoFra: LocalDate,
        val datoTil: LocalDate,
        val aktivitet: String,
        val periodeType: String)

data class PeriodeWrapper(val perioder: List<Vedtaksperiode>)
data class InntektWrapper(val inntekter: List<Inntektsperiode>)