package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto

data class BlankettPdfRequest(
        val personopplysninger: PersonopplysningerDto,
        val inngangsvilkår: InngangsvilkårDto
)

data class PersonopplysningerDto(
        val navn: String,
        val personIdent: String
)