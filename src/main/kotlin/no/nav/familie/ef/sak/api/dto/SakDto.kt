package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.ef.søknad.Søknad
import java.util.*

data class SakDto(val id: UUID,
                  val søknad: Søknad,
                  val saksnummer: String,
                  val journalpostId: String,
                  val overgangsstønad: OvergangsstønadDto)
