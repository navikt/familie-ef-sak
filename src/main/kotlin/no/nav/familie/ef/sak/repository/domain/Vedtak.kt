package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.beregning.ResultatType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.UUID


data class Vedtak(@Id
                  val behandlingId: UUID,
                  val resultatType: ResultatType,
                  val periodeBegrunnelse: String? = null,
                  val inntektBegrunnelse: String? = null,
                  @Column("avsla_begrunnelse")
                  val avslåBegrunnelse: String? = null,
                  val perioder: PeriodeWrapper? = null,
                  val inntekter: InntektWrapper? = null,
                  val saksbehandlerIdent: String? = null,
                  @Column("opphor_fom")
                  val opphørFom: LocalDate? = null,
                  val beslutterIdent: String? = null)

data class Vedtaksperiode(
        val datoFra: LocalDate,
        val datoTil: LocalDate,
        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType)

data class PeriodeWrapper(val perioder: List<Vedtaksperiode>)
data class InntektWrapper(val inntekter: List<Inntektsperiode>)

enum class VedtaksperiodeType {
    PERIODE_FØR_FØDSEL,
    HOVEDPERIODE,
}

enum class AktivitetType {
    IKKE_AKTIVITETSPLIKT,
    BARN_UNDER_ETT_ÅR,
    FORSØRGER_I_ARBEID,
    FORSØRGER_I_UTDANNING,
    FORSØRGER_REELL_ARBEIDSSØKER,
    FORSØRGER_ETABLERER_VIRKSOMHET,
    BARNET_SÆRLIG_TILSYNSKREVENDE,
    FORSØRGER_MANGLER_TILSYNSORDNING,
    FORSØRGER_ER_SYK,
    BARNET_ER_SYKT,
}