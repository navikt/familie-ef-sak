package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.api.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.BarnMedSamværSøknadsgrunnlagDto
import no.nav.familie.ef.sak.api.dto.AnnenForelderDto
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.søknad.AnnenForelder
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer

object BarnMedSamværMapper {

    fun tilDto(pdlBarn: Map<String, PdlBarn>,
               barneforeldre: Map<String, PdlAnnenForelder>,
               søknad: SøknadsskjemaOvergangsstønad): List<BarnMedSamværDto> {

        val alleBarn: List<MatchetBarn> = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, pdlBarn)
                .sortedByDescending {
                    if (it.fødselsnummer != null)
                        Fødselsnummer(it.fødselsnummer).fødselsdato
                    else it.søknadsbarn.fødselTermindato
                }

        return alleBarn.map { barn ->
            val fnr = barn.pdlBarn?.familierelasjoner?.firstOrNull {
                it.relatertPersonsIdent != søknad.fødselsnummer && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
            }?.relatertPersonsIdent
                      ?: barn.søknadsbarn.annenForelder?.person?.fødselsnummer
            val pdlAnnenForelder = barneforeldre[fnr]

            tilDtoPerBarn(barn, pdlAnnenForelder, fnr)
        }
    }

    private fun tilDtoPerBarn(matchetBarn: MatchetBarn,
                              pdlAnnenForelder: PdlAnnenForelder?,
                              annenForelderFnr: String?): BarnMedSamværDto {
        val søknadsbarn = matchetBarn.søknadsbarn
        val samvær = søknadsbarn.samvær
        return BarnMedSamværDto(
                barnId = søknadsbarn.id.toString(),
                søknadsgrunnlag = BarnMedSamværSøknadsgrunnlagDto(
                        navn = søknadsbarn.navn,
                        fødselsnummer = søknadsbarn.fødselsnummer,
                        fødselTermindato = søknadsbarn.fødselTermindato,
                        erBarnetFødt = søknadsbarn.erBarnetFødt,
                        harSammeAdresse = søknadsbarn.harSkalHaSammeAdresse,
                        skalBoBorHosSøker = søknadsbarn.skalBoHosSøker,
                        forelder = søknadsbarn.annenForelder?.let { tilAnnenForelderDto(it) },
                        ikkeOppgittAnnenForelderBegrunnelse = søknadsbarn.annenForelder?.ikkeOppgittAnnenForelderBegrunnelse,
                        spørsmålAvtaleOmDeltBosted = samvær?.spørsmålAvtaleOmDeltBosted,
                        skalAnnenForelderHaSamvær = samvær?.skalAnnenForelderHaSamvær,
                        harDereSkriftligAvtaleOmSamvær = samvær?.harDereSkriftligAvtaleOmSamvær,
                        hvordanPraktiseresSamværet = samvær?.hvordanPraktiseresSamværet,
                        borAnnenForelderISammeHus = samvær?.borAnnenForelderISammeHus,
                        borAnnenForelderISammeHusBeskrivelse = samvær?.borAnnenForelderISammeHusBeskrivelse,
                        harDereTidligereBoddSammen = samvær?.harDereTidligereBoddSammen,
                        nårFlyttetDereFraHverandre = samvær?.nårFlyttetDereFraHverandre,
                        hvorMyeErDuSammenMedAnnenForelder = samvær?.hvorMyeErDuSammenMedAnnenForelder,
                        beskrivSamværUtenBarn = samvær?.beskrivSamværUtenBarn,
                ),
                registergrunnlag = BarnMedSamværRegistergrunnlagDto(
                        navn = matchetBarn.pdlBarn?.navn?.gjeldende()?.visningsnavn(),
                        fødselsnummer = matchetBarn.fødselsnummer,
                        harSammeAdresse = matchetBarn.pdlBarn?.let {
                            AdresseHjelper.borPåSammeAdresse(it, pdlAnnenForelder?.bostedsadresse ?: emptyList())
                        },
                        forelder = pdlAnnenForelder?.let { tilAnnenForelderDto(it, annenForelderFnr) }
                )
        )
    }

    private fun tilAnnenForelderDto(annenForelder: AnnenForelder): AnnenForelderDto {
        return AnnenForelderDto(
                navn = annenForelder.person?.navn,
                fødselsnummer = annenForelder.person?.fødselsnummer,
                fødselsdato = annenForelder.person?.fødselsdato,
                bosattINorge = annenForelder.bosattNorge,
                land = annenForelder.land
        )

    }

    private fun tilAnnenForelderDto(pdlAnnenForelder: PdlAnnenForelder, annenForelderFnr: String?): AnnenForelderDto {
        return AnnenForelderDto(
                navn = pdlAnnenForelder.navn.gjeldende().visningsnavn(),
                fødselsnummer = annenForelderFnr,
                fødselsdato = pdlAnnenForelder.fødsel.gjeldende()?.fødselsdato,
                bosattINorge = pdlAnnenForelder.bostedsadresse.gjeldende()?.utenlandskAdresse?.let { false } ?: true,
                land = pdlAnnenForelder.bostedsadresse.gjeldende()?.utenlandskAdresse?.landkode
        )

    }
}