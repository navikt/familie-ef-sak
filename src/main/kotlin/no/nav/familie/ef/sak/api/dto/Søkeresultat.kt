package no.nav.familie.ef.sak.api.dto

import java.util.*


data class Søkeresultat(val personIdent: String, val visningsnavn: String, val kjønn: Kjønn, val fagsaker: List<UUID> )