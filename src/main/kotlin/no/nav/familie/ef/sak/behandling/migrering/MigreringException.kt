package no.nav.familie.ef.sak.behandling.migrering

class MigreringException(val årsak: String, val type: MigreringExceptionType) : RuntimeException(årsak)

enum class MigreringExceptionType(val kanGåVidereTilJournalføring: Boolean = false) {
    ÅPEN_SAK,
    FLERE_IDENTER,
    FLERE_AKTIVE_PERIODER,
    HAR_ALLEREDE_BEHANDLINGER,
    FEIL_STØNADSTYPE,
    FLERE_IDENTER_VEDTAK,
    ALLEREDE_MIGRERT,
    MANGLER_PERIODER(kanGåVidereTilJournalføring = true),
    MANGLER_PERIODER_MED_BELØP,
    FEIL_FOM_DATO,
    FEIL_TOM_DATO,
    ELDRE_PERIODER(kanGåVidereTilJournalføring = true),
    SIMULERING_FEILUTBETALING,
    SIMULERING_ETTERBETALING,
    SIMULERING_DEBIT_TREKK,
    BELØP_0,
}
