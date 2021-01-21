package no.nav.familie.ef.sak.api.dto

data class TotrinnskontrollDto(val godkjent: Boolean,
                               val begrunnelse: String? = null)

data class TotrinnskontrollStatusDto(val status: TotrinnkontrollStatus,
                                     val begrunnelse: String? = null)

enum class TotrinnkontrollStatus {
    TOTRINNSKONTROLL_UNDERKJENT,
    KAN_FATTE_VEDTAK,
    IKKE_AUTORISERT,
    UAKTUELT
}