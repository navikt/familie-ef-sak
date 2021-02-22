package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import java.util.*


data class Søkeresultat(val personIdent: String, val visningsnavn: String, val kjønn: Kjønn, val fagsaker: List<FagsakForSøkeresultat>)


data class FagsakForSøkeresultat(val fagsakId: UUID, val stønadstype: Stønadstype)