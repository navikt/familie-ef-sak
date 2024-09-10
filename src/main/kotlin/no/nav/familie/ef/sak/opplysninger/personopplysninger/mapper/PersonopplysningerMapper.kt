package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Personopplysninger
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AnnenForelderMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.DeltBostedDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FullmaktDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.SivilstandDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.VergemålDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class PersonopplysningerMapper(
    private val adresseMapper: AdresseMapper,
    private val statsborgerskapMapper: StatsborgerskapMapper,
    private val innflyttingUtflyttingMapper: InnflyttingUtflyttingMapper,
) {
    fun tilPersonopplysninger(
        grunnlagsdata: Personopplysninger,
        grunnlagsdataOpprettet: LocalDateTime,
        egenAnsatt: Boolean,
        søkerIdenter: PdlIdenter,
    ): PersonopplysningerDto {
        val søker = grunnlagsdata.søker
        val annenForelderMap = grunnlagsdata.annenForelder.associateBy { it.personIdent }

        val gjeldendePersonIdent = søkerIdenter.gjeldende().ident
        return PersonopplysningerDto(
            adressebeskyttelse =
                søker.adressebeskyttelse
                    ?.let { Adressebeskyttelse.valueOf(it.gradering.name) },
            folkeregisterpersonstatus =
                søker.folkeregisterpersonstatus
                    .gjeldende()
                    ?.let { Folkeregisterpersonstatus.fraPdl(it) },
            fødselsdato = søker.fødsel?.gjeldende()?.foedselsdato,
            dødsdato = søker.dødsfall?.dødsdato,
            navn = NavnDto.fraNavn(søker.navn),
            kjønn = KjønnMapper.tilKjønn(søker.kjønn),
            personIdent = gjeldendePersonIdent,
            statsborgerskap = statsborgerskapMapper.map(søker.statsborgerskap),
            sivilstand =
                søker.sivilstand
                    .map {
                        SivilstandDto(
                            type = Sivilstandstype.valueOf(it.type.name),
                            gyldigFraOgMed = it.gyldigFraOgMed ?: it.bekreftelsesdato,
                            relatertVedSivilstand = it.relatertVedSivilstand,
                            navn = it.navn,
                            dødsdato = it.dødsfall?.dødsdato,
                            erGjeldende = !it.metadata.historisk,
                        )
                    }.sortedWith(compareByDescending<SivilstandDto> { it.erGjeldende }.thenByDescending { it.gyldigFraOgMed }),
            adresse = tilAdresser(søker),
            fullmakt =
                søker.fullmakt
                    .map {
                        FullmaktDto(
                            gyldigFraOgMed = it.gyldigFraOgMed,
                            gyldigTilOgMed = it.gyldigTilOgMed,
                            motpartsPersonident = it.motpartsPersonident,
                            navn = it.navn,
                            områder = it.områder?.let { it.map { område -> mapOmråde(område) } } ?: emptyList(),
                        )
                    }.sortedByDescending { it.gyldigFraOgMed },
            egenAnsatt = egenAnsatt,
            barn =
                grunnlagsdata.barn
                    .map {
                        mapBarn(
                            it,
                            søkerIdenter.identer(),
                            søker.bostedsadresse,
                            annenForelderMap,
                            grunnlagsdataOpprettet.toLocalDate(),
                        )
                    }.sortedBy { it.fødselsdato },
            innflyttingTilNorge = innflyttingUtflyttingMapper.mapInnflytting(søker.innflyttingTilNorge),
            utflyttingFraNorge = innflyttingUtflyttingMapper.mapUtflytting(søker.utflyttingFraNorge),
            oppholdstillatelse = OppholdstillatelseMapper.map(søker.opphold),
            vergemål = mapVergemål(søker),
        )
    }

    fun tilPersonopplysninger(
        grunnlagsdataMedMetadata: GrunnlagsdataMedMetadata,
        egenAnsatt: Boolean,
        søkerIdenter: PdlIdenter,
    ): PersonopplysningerDto =
        tilPersonopplysninger(
            grunnlagsdata = grunnlagsdataMedMetadata.grunnlagsdata.tilPersonopplysninger(),
            grunnlagsdataOpprettet = grunnlagsdataMedMetadata.opprettetTidspunkt,
            egenAnsatt = egenAnsatt,
            søkerIdenter = søkerIdenter,
        )

    private fun mapOmråde(område: String): String =
        when (område) {
            "*" -> "ALLE"
            else -> område
        }

    private fun mapVergemål(søker: Søker) =
        søker.vergemaalEllerFremtidsfullmakt.filter { it.type != "stadfestetFremtidsfullmakt" }.map {
            VergemålDto(
                embete = it.embete,
                type = it.type,
                motpartsPersonident = it.vergeEllerFullmektig.motpartsPersonident,
                navn = it.vergeEllerFullmektig.navn?.visningsnavn(),
                omfang = it.vergeEllerFullmektig.omfang,
            )
        }

    fun tilAdresser(søker: Søker): List<AdresseDto> {
        val adresser =
            søker.bostedsadresse.map(adresseMapper::tilAdresse) +
                søker.kontaktadresse.map(adresseMapper::tilAdresse) +
                søker.oppholdsadresse.map(adresseMapper::tilAdresse)

        return AdresseHjelper.sorterAdresser(adresser)
    }

    private fun mapBarn(
        barn: BarnMedIdent,
        søkerIdenter: Set<String>,
        bostedsadresserForelder: List<Bostedsadresse>,
        annenForelderMap: Map<String, AnnenForelderMedIdent>,
        grunnlagsdataOpprettet: LocalDate,
    ): BarnDto {
        val annenForelderIdent =
            barn.forelderBarnRelasjon
                .find {
                    !søkerIdenter.contains(it.relatertPersonsIdent) && it.relatertPersonsRolle != Familierelasjonsrolle.BARN
                }?.relatertPersonsIdent

        feilHvis(barn.deltBosted.filter { !it.metadata.historisk }.size > 1) { "Fant mer enn en ikke-historisk delt bosted." }
        val deltBostedDto =
            barn.deltBosted.map { DeltBostedDto(it.startdatoForKontrakt, it.sluttdatoForKontrakt, it.metadata.historisk) }

        return BarnDto(
            personIdent = barn.personIdent,
            navn = barn.navn.visningsnavn(),
            annenForelder =
                annenForelderIdent?.let {
                    val annenForelder = annenForelderMap[it]
                    AnnenForelderMinimumDto(
                        personIdent = it,
                        navn = annenForelder?.navn?.visningsnavn() ?: "Finner ikke navn",
                        dødsdato = annenForelder?.dødsfall?.gjeldende()?.dødsdato,
                        bostedsadresse =
                            annenForelder
                                ?.bostedsadresse
                                ?.gjeldende()
                                ?.let { adresseMapper.tilAdresse(it).visningsadresse },
                    )
                },
            adresse = barn.bostedsadresse.map(adresseMapper::tilAdresse),
            borHosSøker = AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, bostedsadresserForelder),
            deltBosted = deltBostedDto,
            harDeltBostedNå = AdresseHjelper.harDeltBosted(barn, grunnlagsdataOpprettet),
            fødselsdato = barn.fødsel.gjeldende().foedselsdato,
            dødsdato = barn.dødsfall.gjeldende()?.dødsdato,
        )
    }
}
