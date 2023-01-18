package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

import no.nav.familie.ef.sak.cucumber.domeneparser.Domenenøkkel

enum class BeregningBarnetilsynDomenebegrep(val nøkkel: String) : Domenenøkkel {
    PERIODEUTGIFT("Periodeutgift"),
    KONTANTSTØTTEBELØP("Kontantstøttebeløp"),
    TILLEGSSTØNADBELØP("Tillegsstønadbeløp"),
    ANTALL_BARN("Antall barn"),
    PERIODEDATO("Periodedato"),
    FRA_MND("Fra måned"),
    TIL_OG_MED_MND("Til og med måned"),
    BELØP("Beløp"),
    HAR_KONTANTSTØTTE("Har kontantstøtte"),
    HAR_TILLEGGSSTØNAD("Har tilleggsstønad"),
    PERIODETYPE("Periodetype"),
    ;

    override fun nøkkel(): String {
        return nøkkel
    }
}
