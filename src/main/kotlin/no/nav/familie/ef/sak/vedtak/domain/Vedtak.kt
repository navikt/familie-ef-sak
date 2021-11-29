package no.nav.familie.ef.sak.vedtak.domain

import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
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
                  @Column("avsla_arsak")
                  val avslåÅrsak: AvslagÅrsak? = null,
                  val perioder: PeriodeWrapper? = null,
                  val inntekter: InntektWrapper? = null,
                  val saksbehandlerIdent: String? = null,
                  @Column("opphor_fom")
                  val opphørFom: LocalDate? = null,
                  val beslutterIdent: String? = null,
                  val brevmottakere: BrevmottakereWrapper? = null)

data class Vedtaksperiode(
        val datoFra: LocalDate,
        val datoTil: LocalDate,
        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType)

data class PeriodeWrapper(val perioder: List<Vedtaksperiode>)
data class InntektWrapper(val inntekter: List<Inntektsperiode>)
data class BrevmottakereWrapper(val mottakere: List<Brevmottaker>)


enum class VedtaksperiodeType {
    FORLENGELSE,
    HOVEDPERIODE,
    PERIODE_FØR_FØDSEL,
    UTVIDELSE,
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
    UTVIDELSE_FORSØRGER_I_UTDANNING,
    UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE,
    FORLENGELSE_MIDLERTIDIG_SYKDOM,
    FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
    FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER,
    FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM,
    FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING,
    FORLENGELSE_STØNAD_PÅVENTE_UTDANNING,
    FORLENGELSE_STØNAD_UT_SKOLEÅRET,
}

enum class AvslagÅrsak {
    VILKÅR_IKKE_OPPFYLT,
    BARN_OVER_ÅTTE_ÅR,
    STØNADSTID_OPPBRUKT,
    MANGLENDE_OPPLYSNINGER,
}