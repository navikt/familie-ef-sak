package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.dto.SøknadDatoerDto
import no.nav.familie.ef.sak.api.dto.VilkårDto

data class BlankettPdfRequest(
        val personopplysninger: PersonopplysningerDto,
        val vilkår: VilkårDto,
        val vedtak: VedtakDto,
        val søknadsdatoer: SøknadDatoerDto
)

data class PersonopplysningerDto(
        val navn: String,
        val personIdent: String
)
