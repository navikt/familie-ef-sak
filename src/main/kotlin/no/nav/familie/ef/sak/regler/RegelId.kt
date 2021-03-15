package no.nav.familie.ef.sak.regler

interface RegelId {

    val id: String
}

interface RegelIdMedBeskrivelse : RegelId {

    val beskrivelse: String
}

object SluttNode : RegelId {
    override val id: String = "SLUTT_NODE"
}