package no.nav.familie.ef.sak.api.dto

import java.util.*

data class SakSøkDto(
        val sakId: UUID,
        val personIdent: String,
        val navn: NavnDto,
        val kjønn: Kjønn,
        val adressebeskyttelse: Adressebeskyttelse,
        val folkeregisterpersonstatus: Folkeregisterpersonstatus?
)

@Suppress("unused")
enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

@Suppress("unused")
enum class Folkeregisterpersonstatus(private val pdlKode: String) {
    BOSATT("bosatt"),
    UTFLYTTET("utflyttet"),
    FORSVUNNET("forsvunnet"),
    DOED("doed"),
    OPPHOERT("opphoert"),
    FOEDSELSREGISTRERT("foedselsregistrert"),
    MIDLERTIDIG("midlertidig"),
    INAKTIV("inaktiv"),
    UKJENT("ukjent");

    companion object {
        private val map = values().associateBy(Folkeregisterpersonstatus::pdlKode)
        fun frånPDLKode(pdlKode: String) = map.getOrDefault(pdlKode, UKJENT)
    }
}

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