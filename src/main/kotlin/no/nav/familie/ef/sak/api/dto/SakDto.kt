package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import java.util.*

data class SakDto(val id: UUID,
                  val søknad: SøknadOvergangsstønad,
                  val saksnummer: String,
                  val journalpostId: String,
                  val overgangsstønad: OvergangsstønadDto)
