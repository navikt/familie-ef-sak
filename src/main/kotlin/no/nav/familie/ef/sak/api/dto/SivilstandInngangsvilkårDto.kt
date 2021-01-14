package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import java.time.LocalDate

data class SivilstandInngangsvilkårDto(val søknadsgrunnlag: SivilstandSøknadsgrunnlagDto,
                                       val registergrunnlag: SivilstandRegistergrunnlagDto)

data class SivilstandSøknadsgrunnlagDto(val samlivsbruddsdato: LocalDate?,
                                        val endringSamværsordningDato: LocalDate?,
                                        val fraflytningsdato: LocalDate?,
                                        val erUformeltGift: Boolean?,
                                        val erUformeltSeparertEllerSkilt: Boolean?,
                                        val datoSøktSeparasjon: LocalDate?,
                                        val søktOmSkilsmisseSeparasjon: Boolean?,
                                        val årsakEnslig: EnumTekstverdiMedSvarId?,
                                        val tidligereSamboer: PersonMinimumDto?)

data class SivilstandRegistergrunnlagDto(val type: Sivilstandstype,
                                         val gyldigFraOgMed: LocalDate?)