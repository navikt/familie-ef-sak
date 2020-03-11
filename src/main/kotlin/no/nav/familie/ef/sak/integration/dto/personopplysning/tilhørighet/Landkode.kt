package no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet

data class Landkode(val kode: String) {

    fun erNorge(): Boolean {
        return NORGE == this
    }

    companion object {
        val UDEFINERT = Landkode("UDEFINERT")
        val NORGE = Landkode("NOR")
    }
}