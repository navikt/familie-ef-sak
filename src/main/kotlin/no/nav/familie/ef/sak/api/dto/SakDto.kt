package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import java.util.*

//Mulig vurdere om denne skal være generisk og at feltet overgangsstønad skal bytte navn
data class SakDto(val id: UUID,
                  val søknad: SøknadOvergangsstønad,
                  val saksnummer: String,
                  val journalpostId: String,
                  val overgangsstønad: OvergangsstønadDto)
