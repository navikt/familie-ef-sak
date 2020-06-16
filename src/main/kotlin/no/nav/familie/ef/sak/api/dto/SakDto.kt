package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.ef.søknad.Søknad

data class SakDto(val søknad: Søknad, val saksnummer: String, val journalpostId: String)
