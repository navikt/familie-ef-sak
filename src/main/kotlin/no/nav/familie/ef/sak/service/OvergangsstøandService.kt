package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.OvergangsstønadDto
import no.nav.familie.ef.sak.mapper.OvergangsstønadMapper
import no.nav.familie.kontrakter.ef.søknad.Søknad
import org.springframework.stereotype.Service

@Service
class OvergangsstøandService {

    fun lagOvergangsstønad(søknad: Søknad): OvergangsstønadDto {
        val aktivitetsplikt = null//TODO - denne kommer som ett resultat av inngangsvilkåret
        val sagtOppEllerRedusertStilling = OvergangsstønadMapper.tilSagtOppEllerRedusertStilling(søknad.situasjon.verdi)
        return OvergangsstønadDto(aktivitet = OvergangsstønadMapper.tilAktivitetDto(søknad, aktivitetsplikt),
                                  sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling)
    }
}
