package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandSøknadsgrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.domene.Søker
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.repository.domain.søknad.Sivilstand

object SivilstandMapper {

    fun tilDto(grunnlagsdata: GrunnlagsdataDomene, sivilstand: Sivilstand): SivilstandInngangsvilkårDto {
        return SivilstandInngangsvilkårDto(søknadsgrunnlag = mapSøknadsgrunnlag(sivilstand),
                                           registergrunnlag = mapRegistergrunnlag(grunnlagsdata.søker))
    }

    fun mapRegistergrunnlag(søker: Søker): SivilstandRegistergrunnlagDto {
        val sivilstand = søker.sivilstand.gjeldende()
        return SivilstandRegistergrunnlagDto(type = Sivilstandstype.valueOf(sivilstand.type.name),
                                             navn = sivilstand.navn,
                                             gyldigFraOgMed = sivilstand.gyldigFraOgMed)
    }

    fun mapSøknadsgrunnlag(sivilstandsdetaljer: Sivilstand): SivilstandSøknadsgrunnlagDto {
        return SivilstandSøknadsgrunnlagDto(samlivsbruddsdato = sivilstandsdetaljer.samlivsbruddsdato,
                                            endringSamværsordningDato = sivilstandsdetaljer.endringSamværsordningDato,
                                            fraflytningsdato = sivilstandsdetaljer.fraflytningsdato,
                                            erUformeltGift = sivilstandsdetaljer.erUformeltGift,
                                            erUformeltSeparertEllerSkilt = sivilstandsdetaljer.erUformeltSeparertEllerSkilt,
                                            datoSøktSeparasjon = sivilstandsdetaljer.datoSøktSeparasjon,
                                            søktOmSkilsmisseSeparasjon = sivilstandsdetaljer.søktOmSkilsmisseSeparasjon,
                                            årsakEnslig = sivilstandsdetaljer.årsakEnslig,
                                            tidligereSamboer = sivilstandsdetaljer.tidligereSamboer?.let {
                                                PersonMinimumMapper.tilDto(it)
                                            }
        )
    }
}