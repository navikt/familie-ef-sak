package no.nav.familie.ef.sak.felles.integration.dto

data class Tilgang(
    val harTilgang: Boolean,
    val begrunnelse: String? = null,
) {

    fun utledÅrsakstekst(): String = when (this.begrunnelse) {
        null -> ""
        else -> "Årsak: ${this.begrunnelse}"
    }
}
