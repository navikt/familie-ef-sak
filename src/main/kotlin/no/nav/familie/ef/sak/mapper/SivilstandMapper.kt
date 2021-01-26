package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandSøknadsgrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.repository.domain.søknad.Sivilstand

object SivilstandMapper {

    fun tilDto(sivilstandsdetaljer: Sivilstand, pdlSøker: PdlSøker): SivilstandInngangsvilkårDto {
        val sivilstand = pdlSøker.sivilstand.gjeldende()
        val registergrunnlag = SivilstandRegistergrunnlagDto(type = Sivilstandstype.valueOf(sivilstand.type.name),
                                                             gyldigFraOgMed = sivilstand.gyldigFraOgMed)

        val søknadsgrunnlag =
                SivilstandSøknadsgrunnlagDto(samlivsbruddsdato = sivilstandsdetaljer.samlivsbruddsdato,
                                             endringSamværsordningDato = sivilstandsdetaljer.endringSamværsordningDato,
                                             fraflytningsdato = sivilstandsdetaljer.fraflytningsdato,
                                             erUformeltGift = sivilstandsdetaljer.erUformeltGift,
                                             erUformeltSeparertEllerSkilt = sivilstandsdetaljer.erUformeltSeparertEllerSkilt,
                                             datoSøktSeparasjon = sivilstandsdetaljer.datoSøktSeparasjon,
                                             søktOmSkilsmisseSeparasjon = sivilstandsdetaljer.søktOmSkilsmisseSeparasjon,
                                             årsakEnslig = sivilstandsdetaljer.årsakEnslig,
                                             tidligereSamboer = sivilstandsdetaljer.tidligereSamboer?.let { PersonMinimumMapper.tilDto(it) }
                )

        return SivilstandInngangsvilkårDto(
                søknadsgrunnlag, registergrunnlag
        )
    }
}