package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AnnenForelderDto
import no.nav.familie.ef.sak.api.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.api.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.BarnMedSamværSøknadsgrunnlagDto
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.søknad.AnnenForelder
import no.nav.familie.ef.sak.repository.domain.søknad.Barn
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad

object BarnMedSamværMapper {

    fun slåSammenBarnMedSamvær(søknadsgrunnlag: List<BarnMedSamværSøknadsgrunnlagDto>,
                               registergrunnlag: List<BarnMedSamværRegistergrunnlagDto>): List<BarnMedSamværDto> {
        val registergrunnlagPaaId = registergrunnlag.associateBy { it.id }
        return søknadsgrunnlag.map {
            val id = it.id
            val registergrunnlag = registergrunnlagPaaId[id] ?: error("Savner registergrunnlag for barn=$id")
            BarnMedSamværDto(barnId = id,
                             søknadsgrunnlag = it,
                             registergrunnlag = registergrunnlag)
        }
    }

    fun mapSøknadsgrunnlag(barn: Set<Barn>): List<BarnMedSamværSøknadsgrunnlagDto> {
        return barn.map(this::mapSøknadsgrunnlag)
    }

    private fun mapSøknadsgrunnlag(søknadsbarn: Barn): BarnMedSamværSøknadsgrunnlagDto {
        val samvær = søknadsbarn.samvær
        return BarnMedSamværSøknadsgrunnlagDto(
                id = søknadsbarn.id,
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
                beskrivSamværUtenBarn = samvær?.beskrivSamværUtenBarn
        )
    }

    fun mapRegistergrunnlag(pdlBarn: Map<String, PdlBarn>,
                            barneforeldre: Map<String, PdlAnnenForelder>,
                            søknad: SøknadsskjemaOvergangsstønad,
                            søkerAdresse: List<Bostedsadresse>): List<BarnMedSamværRegistergrunnlagDto> {

        val alleBarn: List<MatchetBarn> = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, pdlBarn)

        return alleBarn.map { barn ->
            val fnr = barn.pdlBarn?.forelderBarnRelasjon?.firstOrNull {
                it.relatertPersonsIdent != søknad.fødselsnummer && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
            }?.relatertPersonsIdent
                      ?: barn.søknadsbarn.annenForelder?.person?.fødselsnummer
            val pdlAnnenForelder = barneforeldre[fnr]

            mapRegistergrunnlag(barn, søkerAdresse, pdlAnnenForelder, fnr)
        }
    }

    private fun mapRegistergrunnlag(matchetBarn: MatchetBarn,
                                    søkerAdresse: List<Bostedsadresse>,
                                    pdlAnnenForelder: PdlAnnenForelder?,
                                    annenForelderFnr: String?): BarnMedSamværRegistergrunnlagDto {
        return BarnMedSamværRegistergrunnlagDto(
                id = matchetBarn.søknadsbarn.id,
                navn = matchetBarn.pdlBarn?.navn?.gjeldende()?.visningsnavn(),
                fødselsnummer = matchetBarn.fødselsnummer,
                harSammeAdresse = matchetBarn.pdlBarn?.let {
                    AdresseHjelper.borPåSammeAdresse(it, søkerAdresse)
                },
                forelder = pdlAnnenForelder?.let { tilAnnenForelderDto(it, annenForelderFnr) }
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