package no.nav.familie.ef.sak.klage.dto

import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import java.time.LocalDate

data class OpprettKlageDto(
    val mottattDato: LocalDate,
    val klageGjelderTilbakekreving: Boolean,
    val behandlingsårsak: Klagebehandlingsårsak,
)
