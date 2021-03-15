package no.nav.familie.ef.sak.regler

interface RegelId {

    val id: String
}

interface RegelIdMedBeskrivelse : RegelId {

    val beskrivelse: String
}

enum class SluttNode(override val id: String = "SLUTT_NODE") : RegelId {
    SLUTT_NODE
}