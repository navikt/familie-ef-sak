package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import java.time.LocalDate

data class SivilstandInngangsvilkårDto(val søknadsgrunnlag: SivilstandSøknadsgrunnlagDto?,
                                       val registergrunnlag: SivilstandRegistergrunnlagDto)

data class SivilstandSøknadsgrunnlagDto(val samlivsbruddsdato: LocalDate?,
                                        val endringSamværsordningDato: LocalDate?,
                                        val fraflytningsdato: LocalDate?,
                                        val erUformeltGift: Boolean?,
                                        val erUformeltSeparertEllerSkilt: Boolean?,
                                        val datoSøktSeparasjon: LocalDate?,
                                        val søktOmSkilsmisseSeparasjon: Boolean?,
                                        val årsakEnslig: String?,
                                        val tidligereSamboer: PersonMinimumDto?)

data class SivilstandRegistergrunnlagDto(val type: Sivilstandstype,
                                         val navn: String?,
                                         val gyldigFraOgMed: LocalDate?)