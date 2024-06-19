package no.nav.familie.ef.sak.brev.dto

sealed class MellomlagretBrevResponse

data class MellomlagretBrevSanity(
    val brevtype: Brevtype = Brevtype.SANITYBREV,
    val brevmal: String,
    val brevverdier: String?,
) : MellomlagretBrevResponse()

enum class Brevtype {
    FRITEKSTBREV,
    SANITYBREV,
}
