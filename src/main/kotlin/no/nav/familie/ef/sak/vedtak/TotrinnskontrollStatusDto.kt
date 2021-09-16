package no.nav.familie.ef.sak.vedtak

import java.time.LocalDateTime

data class BeslutteVedtakDto(val godkjent: Boolean,
                             val begrunnelse: String? = null)

data class TotrinnskontrollStatusDto(val status: TotrinnkontrollStatus,
                                     val totrinnskontroll: TotrinnskontrollDto? = null)

data class TotrinnskontrollDto(val opprettetAv: String,
                               val opprettetTid: LocalDateTime,
                               val godkjent: Boolean? = null,
                               val begrunnelse: String? = null)

enum class TotrinnkontrollStatus {
    TOTRINNSKONTROLL_UNDERKJENT,
    KAN_FATTE_VEDTAK,
    IKKE_AUTORISERT,
    UAKTUELT
}