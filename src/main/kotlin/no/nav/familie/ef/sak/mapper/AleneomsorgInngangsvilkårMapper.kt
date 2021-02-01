package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AleneomsorgDto
import no.nav.familie.ef.sak.api.dto.AleneomsorgRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.AleneomsorgSøknadsgrunnlagDto
import no.nav.familie.ef.sak.api.dto.AleneomsorgAnnenForelderDto
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.søknad.AnnenForelder
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad

object AleneomsorgInngangsvilkårMapper {

    fun tilDto(pdlBarn: Map<String, PdlBarn>,
               barneforeldre: Map<String, PdlAnnenForelder>,
               søknad: SøknadsskjemaOvergangsstønad): List<AleneomsorgDto> {

        val alleBarn: List<MatchetBarn> = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, pdlBarn)

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
                              annenForelderFnr: String?): AleneomsorgDto {
        val søknadsbarn = matchetBarn.søknadsbarn
        val samvær = søknadsbarn.samvær
        return AleneomsorgDto(
            barneId = søknadsbarn.id.toString(),
            søknadsgrunnlag = AleneomsorgSøknadsgrunnlagDto(
                        navn = søknadsbarn.navn,
                        fødselsnummer = søknadsbarn.fødselsnummer,
                        fødselTermindato = søknadsbarn.fødselTermindato,
                        erBarnetFødt = søknadsbarn.erBarnetFødt,
                        harSammeAdresse = søknadsbarn.harSkalHaSammeAdresse,
                        skalBoBorHosSøker = null, // TODO: må legges til i api og kontrakter
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
            registergrunnlag = AleneomsorgRegistergrunnlagDto(
                        navn = matchetBarn.pdlBarn?.navn?.gjeldende()?.visningsnavn(),
                        fødselsnummer = matchetBarn.fødselsnummer,
                        harSammeAdresse =  matchetBarn.pdlBarn?.let {
                            AdresseHjelper.borPåSammeAdresse(it, pdlAnnenForelder?.bostedsadresse ?: emptyList())
                        },
                        forelder = pdlAnnenForelder?.let { tilAnnenForelderDto(it, annenForelderFnr) }
                )
        )
    }

    private fun tilAnnenForelderDto(annenForelder: AnnenForelder): AleneomsorgAnnenForelderDto {
        return AleneomsorgAnnenForelderDto(
                navn = annenForelder.person?.navn,
                fødselsnummer = annenForelder.person?.fødselsnummer,
                fødselsdato = annenForelder.person?.fødselsdato,
                bosattINorge = annenForelder.bosattNorge,
                land = annenForelder.land
        )

    }

    private fun tilAnnenForelderDto(pdlAnnenForelder: PdlAnnenForelder, annenForelderFnr: String?): AleneomsorgAnnenForelderDto {
        return AleneomsorgAnnenForelderDto(
                navn = pdlAnnenForelder.navn.gjeldende().visningsnavn(),
                fødselsnummer = annenForelderFnr,
                fødselsdato = pdlAnnenForelder.fødsel.gjeldende()?.fødselsdato,
                bosattINorge = pdlAnnenForelder.bostedsadresse.gjeldende()?.utenlandskAdresse?.let { false } ?: true,
                land = pdlAnnenForelder.bostedsadresse.gjeldende()?.utenlandskAdresse?.landkode
        )

    }
}