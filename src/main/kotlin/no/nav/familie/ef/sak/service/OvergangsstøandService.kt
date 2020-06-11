package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.OvergangsstønadDto
import no.nav.familie.ef.sak.mapper.OvergangsstønadMapper
import org.springframework.stereotype.Service
import java.util.*

@Service
class OvergangsstøandService(val sakService: SakService) {


    fun hentOvergangsstønad(id: UUID): OvergangsstønadDto {
        val søknad = sakService.hentSak(id).søknad
        val aktivitetsplikt = null//TODO - denne kommer som ett resultat av inngangsvilkåret
        return OvergangsstønadDto(sakId = id,
                                  aktivitet = OvergangsstønadMapper.tilAktivitetDto(søknad, aktivitetsplikt),
                                  sagtOppEllerRedusertStilling = OvergangsstønadMapper.tilSagtOppEllerRedusertStilling(søknad.situasjon.verdi)
        )
    }
}
