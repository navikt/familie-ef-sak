package no.nav.familie.ef.sak.domene

import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker

data class SøkerMedBarn(val søkerIdent: String, val søker: PdlSøker, val barn: Map<String, PdlBarn>) {

    fun identifikatorer(): List<String> = listOf(søkerIdent) + barn.keys

}
