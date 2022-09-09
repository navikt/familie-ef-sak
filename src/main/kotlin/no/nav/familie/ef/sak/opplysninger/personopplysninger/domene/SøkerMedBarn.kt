package no.nav.familie.ef.sak.opplysninger.personopplysninger.domene

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker

data class SøkerMedBarn(val søkerIdent: String, val søker: PdlSøker, val barn: Map<String, PdlPersonForelderBarn>) {

    fun identifikatorer(): List<String> = listOf(søkerIdent) + barn.keys
}
