package no.nav.familie.ef.sak.brev.dto

import no.nav.familie.ef.sak.brev.domain.Fritekstbrev

sealed class MellomlagretBrevResponse

data class MellomlagretBrevSanity(val brevtype: Brevtype = Brevtype.SANITYBREV, val brevmal: String, val brevverdier: String?) :
        MellomlagretBrevResponse()

data class MellomlagretBrevFritekst(val brevtype: Brevtype = Brevtype.FRITEKSTBREV,
                                    val brev: Fritekstbrev,
                                    val brevType: FritekstBrevKategori,
                                    val brevmal: String = "Fritekstbrev") :
        MellomlagretBrevResponse()

enum class Brevtype {
    FRITEKSTBREV,
    SANITYBREV
}
