package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.AnnenForelder
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Barn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.vilkår.dto.AnnenForelderDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværSøknadsgrunnlagDto

object BarnMedSamværMapper {

    fun slåSammenBarnMedSamvær(søknadsgrunnlag: List<BarnMedSamværSøknadsgrunnlagDto>,
                               registergrunnlag: List<BarnMedSamværRegistergrunnlagDto>): List<BarnMedSamværDto> {
        val registergrunnlagPaaId = registergrunnlag.associateBy { it.id }
        return søknadsgrunnlag.map {
            val id = it.id
            BarnMedSamværDto(barnId = id,
                             søknadsgrunnlag = it,
                             registergrunnlag = registergrunnlagPaaId[id] ?: error("Savner registergrunnlag for barn=$id"))
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
                fødselTermindato = søknadsbarn.fødselTermindato,
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

    fun mapRegistergrunnlag(barnMedIdent: List<BarnMedIdent>,
                            barneforeldre: List<AnnenForelderMedIdent>,
                            søknad: SøknadsskjemaOvergangsstønad,
                            søkerAdresse: List<Bostedsadresse>): List<BarnMedSamværRegistergrunnlagDto> {

        val alleBarn: List<MatchetBarn> = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, barnMedIdent)
        val forelderMap = barneforeldre.associateBy { it.personIdent }

        return alleBarn.map { barn ->
            val fnr = barn.barn?.forelderBarnRelasjon?.firstOrNull {
                it.relatertPersonsIdent != søknad.fødselsnummer && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
            }?.relatertPersonsIdent
                      ?: barn.søknadsbarn.annenForelder?.person?.fødselsnummer
            val pdlAnnenForelder = forelderMap[fnr]

            mapRegistergrunnlag(barn, søkerAdresse, pdlAnnenForelder, fnr)
        }
    }

    private fun mapRegistergrunnlag(matchetBarn: MatchetBarn,
                                    søkerAdresse: List<Bostedsadresse>,
                                    pdlAnnenForelder: AnnenForelderMedIdent?,
                                    annenForelderFnr: String?): BarnMedSamværRegistergrunnlagDto {
        return BarnMedSamværRegistergrunnlagDto(
                id = matchetBarn.søknadsbarn.id,
                navn = matchetBarn.barn?.navn?.visningsnavn(),
                fødselsnummer = matchetBarn.fødselsnummer,
                harSammeAdresse = matchetBarn.barn?.let {
                    AdresseHjelper.borPåSammeAdresse(it, søkerAdresse)
                },
                forelder = pdlAnnenForelder?.let { tilAnnenForelderDto(it, annenForelderFnr) },
                dødsdato = matchetBarn.barn?.dødsfall?.elementAt(0)?.dødsdato,
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

    private fun tilAnnenForelderDto(pdlAnnenForelder: AnnenForelderMedIdent, annenForelderFnr: String?): AnnenForelderDto {
        return AnnenForelderDto(
                navn = pdlAnnenForelder.navn.visningsnavn(),
                fødselsnummer = annenForelderFnr,
                fødselsdato = pdlAnnenForelder.fødsel.gjeldende().fødselsdato,
                dødsfall = pdlAnnenForelder.dødsfall.gjeldende()?.dødsdato,
                bosattINorge = pdlAnnenForelder.bostedsadresse.gjeldende()?.utenlandskAdresse?.let { false } ?: true,
                land = pdlAnnenForelder.bostedsadresse.gjeldende()?.utenlandskAdresse?.landkode
        )

    }
}