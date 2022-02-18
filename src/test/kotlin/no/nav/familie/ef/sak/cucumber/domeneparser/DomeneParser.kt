package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

object DomeneParser: BasisDomeneParser() {
}

enum class Domenebegrep(val nøkkel: String) : Domenenøkkel {
    BEHANDLING_ID("BehandlingId"),
    FRA_OG_MED_DATO("Fra og med dato"),
    TIL_OG_MED_DATO("Til og med dato");

    override fun nøkkel(): String {
        return nøkkel
    }
}
