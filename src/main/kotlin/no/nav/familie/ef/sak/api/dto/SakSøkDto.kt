package no.nav.familie.ef.sak.api.dto

import java.util.*

data class SakSøkDto(
        val navn: NavnDto,
        val personIdent: String,
        val kjønn: Kjønn,
        val sakId: UUID
)

@Suppress("unused")
enum class Kjønn {

    KVINNE,
    MANN,
    UKJENT
}

data class NavnDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String
) {

    @Suppress("unused") val visningsnavn: String = when (mellomnavn) {
        null -> "$fornavn $etternavn"
        else -> "$fornavn $mellomnavn $etternavn"
    }
}