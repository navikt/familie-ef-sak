package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårDto
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak

data class BlankettPdfRequest(
        val behandling: BlankettPdfBehandling,
        val personopplysninger: PersonopplysningerDto,
        val vilkår: VilkårDto,
        val vedtak: VedtakDto,
        val søknadsdatoer: SøknadDatoerDto
)

data class BlankettPdfBehandling(
        val årsak: BehandlingÅrsak
)

data class PersonopplysningerDto(
        val navn: String,
        val personIdent: String
)
