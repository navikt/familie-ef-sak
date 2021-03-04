package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.dto.VilkårDto

data class BlankettPdfRequest(
        val personopplysninger: PersonopplysningerDto,
        val inngangsvilkår: VilkårDto
)

data class PersonopplysningerDto(
        val navn: String,
        val personIdent: String
)