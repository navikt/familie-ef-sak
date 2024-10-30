package no.nav.familie.ef.sak.cucumber.domeneparser

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.førsteDagenIMånedenEllerDefault
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.sisteDagenIMånedenEllerDefault

enum class Domenebegrep(
    val nøkkel: String,
) : Domenenøkkel {
    BEHANDLING_ID("BehandlingId"),
    FRA_OG_MED_DATO("Fra og med dato"),
    TIL_OG_MED_DATO("Til og med dato"),
    ;

    override fun nøkkel(): String = nøkkel
}

fun parseFraOgMed(rad: Map<String, String>) = parseValgfriÅrMånedEllerDato(Domenebegrep.FRA_OG_MED_DATO, rad).førsteDagenIMånedenEllerDefault()

fun parseTilOgMed(rad: Map<String, String>) = parseValgfriÅrMånedEllerDato(Domenebegrep.TIL_OG_MED_DATO, rad).sisteDagenIMånedenEllerDefault()
