package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.PersonMinimumMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.vilkår.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandSøknadsgrunnlagDto

object SivilstandMapper {

    fun tilDto(grunnlagsdata: GrunnlagsdataDomene, sivilstand: Sivilstand?): SivilstandInngangsvilkårDto {
        return SivilstandInngangsvilkårDto(søknadsgrunnlag = sivilstand?.let { mapSøknadsgrunnlag(it) },
                                           registergrunnlag = mapRegistergrunnlag(grunnlagsdata.søker))
    }

    private fun mapRegistergrunnlag(søker: Søker): SivilstandRegistergrunnlagDto {
        val sivilstand = søker.sivilstand.gjeldende()
        return SivilstandRegistergrunnlagDto(type = Sivilstandstype.valueOf(sivilstand.type.name),
                                             navn = sivilstand.navn,
                                             gyldigFraOgMed = sivilstand.gyldigFraOgMed)
    }

    private fun mapSøknadsgrunnlag(sivilstandsdetaljer: Sivilstand): SivilstandSøknadsgrunnlagDto {
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