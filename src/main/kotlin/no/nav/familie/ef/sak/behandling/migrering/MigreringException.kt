package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import org.springframework.http.HttpStatus

class MigreringException(val årsak: String, val type: MigreringExceptionType) : ApiFeil(årsak, HttpStatus.BAD_REQUEST)

enum class MigreringExceptionType(val kanGåVidereTilJournalføring: Boolean = false) {
    ÅPEN_SAK,
    FLERE_IDENTER,
    FLERE_AKTIVE_PERIODER,
    INGEN_AKTIV_STØNAD,
    HAR_ALLEREDE_BEHANDLINGER,
    IKKE_FERDIGSTILT_BEHANDLING,
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
    SIMULERING_DEBET_TREKK,
    BELØP_0,
}
