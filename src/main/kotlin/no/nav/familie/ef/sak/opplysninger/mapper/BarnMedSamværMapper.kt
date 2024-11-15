package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.DeltBostedDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.AnnenForelder
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Barnepassordning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.vilkår.dto.AnnenForelderDto
import no.nav.familie.ef.sak.vilkår.dto.AvstandTilSøkerDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnepassDto
import no.nav.familie.ef.sak.vilkår.dto.BarnepassordningDto
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BarnMedSamværMapper(
    private val adresseMapper: AdresseMapper,
) {
    fun slåSammenBarnMedSamvær(
        søknadsgrunnlag: List<BarnMedSamværSøknadsgrunnlagDto>,
        registergrunnlag: List<BarnMedSamværRegistergrunnlagDto>,
        barnepass: List<BarnepassDto>,
    ): List<BarnMedSamværDto> {
        val registergrunnlagPaaId = registergrunnlag.associateBy { it.id }
        val barnepassPaaId = barnepass.associateBy { it.id }
        return søknadsgrunnlag.map {
            val id = it.id
            BarnMedSamværDto(
                barnId = id,
                søknadsgrunnlag = it,
                registergrunnlag = registergrunnlagPaaId[id] ?: error("Savner registergrunnlag for barn=$id"),
                barnepass = barnepassPaaId[id],
            )
        }
    }

    fun mapSøknadsgrunnlag(
        behandlingBarn: List<BehandlingBarn>,
        søknadBarn: Collection<SøknadBarn>,
    ): List<BarnMedSamværSøknadsgrunnlagDto> {
        val søknadsbarn = søknadBarn.associateBy { it.id }
        return behandlingBarn.map { barn -> mapSøknadsgrunnlag(barn, barn.søknadBarnId?.let { søknadsbarn[it] }) }
    }

    fun mapBarnepass(
        behandlingBarn: List<BehandlingBarn>,
        søknadBarn: Collection<SøknadBarn>,
    ): List<BarnepassDto> {
        val søknadsbarn = søknadBarn.associateBy { it.id }
        return behandlingBarn.map { barn -> mapBarnepass(barn, barn.søknadBarnId?.let { søknadsbarn[it] }) }
    }

    private fun mapBarnepass(
        behandlingBarn: BehandlingBarn,
        søknadBarn: SøknadBarn?,
    ): BarnepassDto =
        BarnepassDto(
            id = behandlingBarn.id,
            skalHaBarnepass = søknadBarn?.skalHaBarnepass ?: false,
            barnepassordninger = søknadBarn?.barnepassordninger?.map(this::mapBarnepassordning) ?: emptyList(),
            årsakBarnepass = søknadBarn?.årsakBarnepass,
        )

    private fun mapBarnepassordning(it: Barnepassordning) =
        BarnepassordningDto(
            type = it.hvaSlagsBarnepassordning,
            navn = it.navn,
            fra = it.datoperiode.fra,
            til = it.datoperiode.til,
            beløp = it.beløp,
        )

    private fun mapSøknadsgrunnlag(
        behandlingBarn: BehandlingBarn,
        søknadsbarn: SøknadBarn?,
    ): BarnMedSamværSøknadsgrunnlagDto {
        val samvær = søknadsbarn?.samvær
        return BarnMedSamværSøknadsgrunnlagDto(
            id = behandlingBarn.id,
            navn = behandlingBarn.navn,
            fødselTermindato = behandlingBarn.fødselTermindato,
            harSammeAdresse = søknadsbarn?.harSkalHaSammeAdresse,
            skalBoBorHosSøker = søknadsbarn?.skalBoHosSøker,
            forelder = søknadsbarn?.annenForelder?.let { tilAnnenForelderDto(it) },
            ikkeOppgittAnnenForelderBegrunnelse = søknadsbarn?.annenForelder?.ikkeOppgittAnnenForelderBegrunnelse,
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
        )
    }

    fun mapRegistergrunnlag(
        personIdentSøker: String,
        barnMedIdent: List<BarnMedIdent>,
        barneforeldre: List<AnnenForelderMedIdent>,
        behandlingBarn: List<BehandlingBarn>,
        søknadsbarn: Collection<SøknadBarn>,
        søkerAdresse: List<Bostedsadresse>,
        grunnlagsdataOpprettet: LocalDate,
    ): List<BarnMedSamværRegistergrunnlagDto> {
        val alleBarn: List<MatchetBehandlingBarn> =
            BarnMatcher.kobleBehandlingBarnOgRegisterBarn(behandlingBarn, barnMedIdent)
        val forelderMap = barneforeldre.associateBy { it.personIdent }

        return alleBarn.map { barn ->
            val fnr = utledFnrForAnnenForelder(barn, personIdentSøker, søknadsbarn)
            val pdlAnnenForelder = forelderMap[fnr]
            mapRegistergrunnlag(barn, søkerAdresse, pdlAnnenForelder, fnr, grunnlagsdataOpprettet)
        }
    }

    private fun utledFnrForAnnenForelder(
        barn: MatchetBehandlingBarn,
        personIdentSøker: String,
        søknadsbarn: Collection<SøknadBarn>,
    ): String? {
        val fnr =
            barn.barn
                ?.forelderBarnRelasjon
                ?.firstOrNull {
                    it.relatertPersonsIdent != personIdentSøker && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
                }?.relatertPersonsIdent
                ?: søknadsbarn
                    .firstOrNull { it.id == barn.behandlingBarn.søknadBarnId }
                    ?.annenForelder
                    ?.person
                    ?.fødselsnummer
        return fnr
    }

    private fun mapRegistergrunnlag(
        matchetBarn: MatchetBehandlingBarn,
        søkerAdresse: List<Bostedsadresse>,
        pdlAnnenForelder: AnnenForelderMedIdent?,
        annenForelderFnr: String?,
        grunnlagsdataOpprettet: LocalDate,
    ): BarnMedSamværRegistergrunnlagDto =
        BarnMedSamværRegistergrunnlagDto(
            id = matchetBarn.behandlingBarn.id,
            navn = matchetBarn.barn?.navn?.visningsnavn(),
            fødselsnummer = matchetBarn.fødselsnummer,
            harSammeAdresse =
                matchetBarn.barn?.let {
                    AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(it, søkerAdresse)
                },
            deltBostedPerioder = matchetBarn.barn?.deltBosted.tilDto(),
            harDeltBostedVedGrunnlagsdataopprettelse =
                AdresseHjelper.harDeltBosted(
                    matchetBarn.barn,
                    grunnlagsdataOpprettet,
                ),
            forelder = pdlAnnenForelder?.let { tilAnnenForelderDto(it, annenForelderFnr, søkerAdresse) },
            dødsdato =
                matchetBarn.barn
                    ?.dødsfall
                    ?.gjeldende()
                    ?.dødsdato,
            fødselsdato =
                matchetBarn.barn
                    ?.fødsel
                    ?.first()
                    ?.fødselsdato,
            folkeregisterpersonstatus =
                matchetBarn.barn
                    ?.folkeregisterpersonstatus
                    ?.gjeldende()
                    ?.let(Folkeregisterpersonstatus::fraPdl),
            adresse =
                matchetBarn.barn
                    ?.bostedsadresse
                    ?.gjeldende()
                    ?.let(adresseMapper::tilAdresse)
                    ?.visningsadresse,
        )

    private fun tilAnnenForelderDto(annenForelder: AnnenForelder): AnnenForelderDto =
        AnnenForelderDto(
            navn = annenForelder.person?.navn,
            fødselsnummer = annenForelder.person?.fødselsnummer,
            fødselsdato = annenForelder.person?.fødselsdato,
            bosattINorge = annenForelder.bosattNorge,
            land = annenForelder.land,
            visningsadresse = null,
            avstandTilSøker = AvstandTilSøkerDto(avstand = null, langAvstandTilSøker = LangAvstandTilSøker.UKJENT),
        )

    private fun tilAnnenForelderDto(
        pdlAnnenForelder: AnnenForelderMedIdent,
        annenForelderFnr: String?,
        søkerAdresse: List<Bostedsadresse>,
    ): AnnenForelderDto =
        AnnenForelderDto(
            navn = pdlAnnenForelder.navn.visningsnavn(),
            fødselsnummer = annenForelderFnr,
            fødselsdato = pdlAnnenForelder.fødsel.first().fødselsdato,
            dødsfall = pdlAnnenForelder.dødsfall.gjeldende()?.dødsdato,
            bosattINorge =
                pdlAnnenForelder.bostedsadresse
                    .gjeldende()
                    ?.utenlandskAdresse
                    ?.let { false } ?: true,
            land =
                pdlAnnenForelder.bostedsadresse
                    .gjeldende()
                    ?.utenlandskAdresse
                    ?.landkode,
            visningsadresse = visningsadresse(pdlAnnenForelder),
            tidligereVedtaksperioder = pdlAnnenForelder.tidligereVedtaksperioder?.tilDto(),
            avstandTilSøker = langAvstandTilSøker(søkerAdresse, pdlAnnenForelder.bostedsadresse.gjeldende()),
        )

    private fun visningsadresse(pdlAnnenForelder: AnnenForelderMedIdent): String? =
        pdlAnnenForelder.bostedsadresse
            .gjeldende()
            ?.let { adresseMapper.tilAdresse(it).visningsadresse }

    private fun langAvstandTilSøker(
        søkerAdresse: List<Bostedsadresse>,
        bostedsadresse: Bostedsadresse?,
    ): AvstandTilSøkerDto =
        bostedsadresse
            ?.vegadresse
            ?.fjerneBoforhold(søkerAdresse.gjeldende()?.vegadresse)
            ?: AvstandTilSøkerDto(avstand = null, langAvstandTilSøker = LangAvstandTilSøker.UKJENT)

    private fun List<DeltBosted>?.tilDto(): List<DeltBostedDto> =
        this?.map {
            DeltBostedDto(it.startdatoForKontrakt, it.sluttdatoForKontrakt, it.metadata.historisk)
        } ?: emptyList()
}
