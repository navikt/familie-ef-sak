package no.nav.familie.ef.sak.brev.dto

@Deprecated("Skal slettes")
data class FrittståendeBrevRequestDto(
    val overskrift: String,
    val avsnitt: List<FrittståendeBrevAvsnitt>,
    val personIdent: String,
    val navn: String,
)
