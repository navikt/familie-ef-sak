package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AleneomsorgDto
import no.nav.familie.ef.sak.api.dto.AleneomsorgRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.AleneomsorgSøknadsgrunnlagDto
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad

object AleneomsorgInngangsvilkårMapper {

    fun tilDto(pdlBarn: Map<String, PdlBarn>, søknad: SøknadsskjemaOvergangsstønad): List<AleneomsorgDto> {

        val alleBarn: List<MatchetBarn> = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, pdlBarn)

        return alleBarn.map { tilDtoPerBarn(it, søknad) }
    }

    fun tilDtoPerBarn(matchetBarn: MatchetBarn, søknad: SøknadsskjemaOvergangsstønad): AleneomsorgDto {
        return AleneomsorgDto(
            barneId = matchetBarn.søknadsbarn.id.toString(),
            søknadsgrunnlag = AleneomsorgSøknadsgrunnlagDto(
                navn = matchetBarn.søknadsbarn.navn,
                fødselsnummer = matchetBarn.søknadsbarn.fødselsnummer,
                fødselTermindato = matchetBarn.søknadsbarn.fødselTermindato,
                skalBoBorHosSøker = matchetBarn.søknadsbarn.harSkalHaSammeAdresse,
                forelder = matchetBarn.søknadsbarn.annenForelder,
                ikkeOppgittAnnenForelderBegrunnelse = matchetBarn.søknadsbarn.annenForelder?.ikkeOppgittAnnenForelderBegrunnelse,
                spørsmålAvtaleOmDeltBosted = matchetBarn.søknadsbarn.samvær?.spørsmålAvtaleOmDeltBosted,
                skalAnnenForelderHaSamvær = matchetBarn.søknadsbarn.samvær?.skalAnnenForelderHaSamvær,
                harDereSkriftligAvtaleOmSamvær = matchetBarn.søknadsbarn.samvær?.harDereSkriftligAvtaleOmSamvær,
                hvordanPraktiseresSamværet = matchetBarn.søknadsbarn.samvær?.hvordanPraktiseresSamværet,
                borAnnenForelderISammeHus = matchetBarn.søknadsbarn.samvær?.borAnnenForelderISammeHus,
                borAnnenForelderISammeHusBeskrivelse = matchetBarn.søknadsbarn.samvær?.borAnnenForelderISammeHusBeskrivelse,
                harDereTidligereBoddSammen = matchetBarn.søknadsbarn.samvær?.harDereTidligereBoddSammen,
                nårFlyttetDereFraHverandre = matchetBarn.søknadsbarn.samvær?.nårFlyttetDereFraHverandre,
                hvorMyeErDuSammenMedAnnenForelder = matchetBarn.søknadsbarn.samvær?.hvorMyeErDuSammenMedAnnenForelder,
                beskrivSamværUtenBarn = matchetBarn.søknadsbarn.samvær?.beskrivSamværUtenBarn,
            ),
            registergrunnlagDto = AleneomsorgRegistergrunnlagDto(
                navn = matchetBarn.pdlBarn?.navn?.gjeldende().visningsnavn(),
                        fødselsnummer = matchetBarn.fødselsnummer,
                        skalBoBorHosSøker = matchetBarn.pdlBarn, // TODO : finn ut av denne
                        forelder = matchetBarn.pdlBarn. // TODO: må hentes uavhengig
            )
        )
    }
}