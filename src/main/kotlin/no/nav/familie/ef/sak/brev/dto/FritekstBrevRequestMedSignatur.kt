package no.nav.familie.ef.sak.brev.dto

data class FritekstBrevRequestMedSignatur(
    val brevFraSaksbehandler: FritekstBrevRequestDto,
    val saksbehandlersignatur: String,
    val enhet: String,
    val erSamværsberegning: Boolean = false,
)

data class FritekstBrevRequestDto(
    val overskrift: String,
    val avsnitt: List<Avsnitt>,
    val personIdent: String,
    val navn: String,
)

data class Avsnitt(
    val deloverskrift: String,
    val innhold: String,
)
