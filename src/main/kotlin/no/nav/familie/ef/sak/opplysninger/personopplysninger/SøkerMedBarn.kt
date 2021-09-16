package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker

data class SøkerMedBarn(val søkerIdent: String, val søker: PdlSøker, val barn: Map<String, PdlBarn>) {

    fun identifikatorer(): List<String> = listOf(søkerIdent) + barn.keys

}
