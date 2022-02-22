package no.nav.familie.ef.sak.behandling.migrering

class MigreringException(val årsak: String, val type: MigreringExceptionType) : RuntimeException(årsak)

enum class MigreringExceptionType {
    ÅPEN_SAK,
    FLERE_IDENTER,
    FLERE_AKTIVE_PERIODER,
    HAR_ALLEREDE_BEHANDLINGER,
    FEIL_STØNADSTYPE,
    FLERE_IDENTER_VEDTAK,
    ALLEREDE_MIGRERT,
    MANGLER_PERIODER,
    MANGLER_PERIODER_MED_BELØP,
    FEIL_FOM_DATO,
    FEIL_TOM_DATO,
    ELDRE_PERIODER,
    SIMULERING_FEILUTBETALING,
    SIMULERING_ETTERBETALING,
    BELØP_0,
}
