package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import org.springframework.http.HttpStatus

class MigreringException(
    val årsak: String,
    val type: MigreringExceptionType,
) : ApiFeil(årsak, HttpStatus.BAD_REQUEST)

enum class MigreringExceptionType {
    ÅPEN_SAK,
    FLERE_IDENTER,
    FLERE_AKTIVE_PERIODER,
    INGEN_AKTIV_STØNAD,
    HAR_ALLEREDE_BEHANDLINGER,
    FEIL_STØNADSTYPE,
    FLERE_IDENTER_VEDTAK,
    ALLEREDE_MIGRERT,
    MANGLER_PERIODER,
    MANGLER_PERIODER_MED_BELØP,
    MANGLER_PERIODER_MED_BELØP_FREM_I_TIDEN,
    FEIL_FOM_DATO,
    FEIL_TOM_DATO,
    ELDRE_PERIODER,
    SIMULERING_FEILUTBETALING,
    SIMULERING_ETTERBETALING,
    SIMULERING_DEBET_TREKK,
    BELØP_0,
}
