package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.vedtak.VedtakDto
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.vilkår.VilkårDto

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
