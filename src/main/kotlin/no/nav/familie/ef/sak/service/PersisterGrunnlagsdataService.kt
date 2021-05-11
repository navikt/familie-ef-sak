package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.MedlemskapDto
import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.domene.Barn
import no.nav.familie.ef.sak.domene.Grunnlagsdata
import no.nav.familie.ef.sak.domene.SivilstandMedNavn
import no.nav.familie.ef.sak.domene.Søker
import no.nav.familie.ef.sak.domene.tilPdlAnnenForelder
import no.nav.familie.ef.sak.domene.tilPdlBarn
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.mapper.AktivitetMapper
import no.nav.familie.ef.sak.mapper.BarnMedSamværMapper
import no.nav.familie.ef.sak.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SagtOppEllerRedusertStillingMapper
import no.nav.familie.ef.sak.mapper.SivilstandMapper
import no.nav.familie.ef.sak.mapper.SivilstandsplanerMapper
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class PersisterGrunnlagsdataService(private val pdlClient: PdlClient,
                                    private val behandlingService: BehandlingService,
                                    private val medlemskapMapper: MedlemskapMapper,
                                    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {


    fun hentGrunnlag(behandlingId: UUID,
                     søknad: SøknadsskjemaOvergangsstønad): VilkårGrunnlagDto {

        val grunnlagsdata = hentGrunnlagsdata(behandlingId)

        val søknad = behandlingService.hentOvergangsstønad(behandlingId)

        val medlemskapRegistergrunnlag =
                medlemskapMapper.mapRegistergrunnlag(søker = grunnlagsdata.søker, medlUnntak = grunnlagsdata.medlUnntak)
        val medlemskapSøknadsgrunnlag = medlemskapMapper.mapSøknadsgrunnlag(medlemskapsdetaljer = søknad.medlemskap)
        val medlemskap = MedlemskapDto(søknadsgrunnlag = medlemskapSøknadsgrunnlag,
                                       registergrunnlag = medlemskapRegistergrunnlag)

        val sivilstandSøknadsgrunnlag = SivilstandMapper.mapSøknadsgrunnlag(sivilstandsdetaljer = søknad.sivilstand)
        val gjeldendeSivilstandMedNavn = grunnlagsdata.søker.sivilstand.gjeldende()
        val sivilstand = SivilstandInngangsvilkårDto(søknadsgrunnlag = sivilstandSøknadsgrunnlag,
                                                     registergrunnlag = SivilstandRegistergrunnlagDto(type = gjeldendeSivilstandMedNavn.type,
                                                                                                      navn = gjeldendeSivilstandMedNavn.navn,
                                                                                                      gyldigFraOgMed = gjeldendeSivilstandMedNavn.gyldigFraOgMed))
        val sivilstandsplaner = SivilstandsplanerMapper.tilDto(sivilstandsplaner = søknad.sivilstandsplaner)

        val pdlBarn = grunnlagsdata.barn.associate { it.personIdent to it.tilPdlBarn() }
        val barneForelder = grunnlagsdata.annenForelder.associate { it.personIdent to it.tilPdlAnnenForelder() }
        val registergrunnlagDataBarn =
                BarnMedSamværMapper.mapRegistergrunnlag(pdlBarn, barneForelder, søknad, grunnlagsdata.søker.bostedsadresse)
        val barnMedSamvær = BarnMedSamværMapper.slåSammenBarnMedSamvær(BarnMedSamværMapper.mapSøknadsgrunnlag(søknad.barn),
                                                                       registergrunnlagDataBarn).sortedByDescending {
            it.registergrunnlag.fødselsnummer?.let { fødsesnummer -> Fødselsnummer(fødsesnummer).fødselsdato }
            ?: it.søknadsgrunnlag.fødselTermindato
        }

        val sagtOppEllerRedusertStilling = SagtOppEllerRedusertStillingMapper.tilDto(situasjon = søknad.situasjon)

        val aktivitet = AktivitetMapper.tilDto(aktivitet = søknad.aktivitet, situasjon = søknad.situasjon, barn = søknad.barn)
        return VilkårGrunnlagDto(medlemskap = medlemskap,
                                 sivilstand = sivilstand,
                                 bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon),
                                 barnMedSamvær = barnMedSamvær,
                                 sivilstandsplaner = sivilstandsplaner,
                                 aktivitet = aktivitet,
                                 sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling)
    }

    fun hentGrunnlagsdata(behandlingId: UUID): Grunnlagsdata {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val personIdent = søknad.fødselsnummer
        val pdlSøker = pdlClient.hentSøker(personIdent)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(søknad, pdlBarn)

        /*TODO VAD SKA VI BRUKE FRA MEDL ?? */
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)

        return Grunnlagsdata(
                søker = Søker(
                        sivilstand = hentNavnForRelatertVedSivilstand(pdlSøker),
                        adressebeskyttelse = pdlSøker.adressebeskyttelse.first(),
                        bostedsadresse = pdlSøker.bostedsadresse,
                        dødsfall = pdlSøker.dødsfall.first(),
                        forelderBarnRelasjon = pdlSøker.forelderBarnRelasjon,
                        fullmakt = pdlSøker.fullmakt,
                        fødsel = pdlSøker.fødsel.first(),
                        folkeregisterpersonstatus = pdlSøker.folkeregisterpersonstatus,
                        innflyttingTilNorge = pdlSøker.innflyttingTilNorge,
                        kjønn = pdlSøker.kjønn.first(),
                        kontaktadresse = pdlSøker.kontaktadresse,
                        navn = pdlSøker.navn,
                        opphold = pdlSøker.opphold,
                        oppholdsadresse = pdlSøker.oppholdsadresse,
                        statsborgerskap = pdlSøker.statsborgerskap,
                        telefonnummer = pdlSøker.telefonnummer,
                        tilrettelagtKommunikasjon = pdlSøker.tilrettelagtKommunikasjon,
                        utflyttingFraNorge = pdlSøker.utflyttingFraNorge,
                        vergemaalEllerFremtidsfullmakt = pdlSøker.vergemaalEllerFremtidsfullmakt
                ),
                annenForelder = barneForeldre.map {
                    AnnenForelderMedIdent(
                            adressebeskyttelse = it.value.adressebeskyttelse,
                            personIdent = it.key,
                            fødsel = it.value.fødsel,
                            bostedsadresse = it.value.bostedsadresse,
                            dødsfall = it.value.dødsfall,
                            innflyttingTilNorge = it.value.innflyttingTilNorge,
                            navn = it.value.navn,
                            opphold = it.value.opphold,
                            oppholdsadresse = it.value.oppholdsadresse,
                            statsborgerskap = it.value.statsborgerskap,
                            utflyttingFraNorge = it.value.utflyttingFraNorge
                    )
                },
                medlUnntak = medlUnntak,
                barn = pdlBarn.map {
                    Barn(fødsel = it.value.fødsel,
                         adressebeskyttelse = it.value.adressebeskyttelse,
                         navn = it.value.navn,
                         bostedsadresse = it.value.bostedsadresse,
                         dødsfall = it.value.dødsfall,
                         deltBosted = it.value.deltBosted,
                         forelderBarnRelasjon = it.value.forelderBarnRelasjon,
                         personIdent = it.key)
                }
        )

    }


    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlBarn> {
        return pdlSøker.forelderBarnRelasjon
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .let { pdlClient.hentBarn(it) }
    }

    private fun hentPdlBarneForeldre(søknad: SøknadsskjemaOvergangsstønad,
                                     barn: Map<String, PdlBarn>): Map<String, PdlAnnenForelder> {
        val barneforeldreFraSøknad = søknad.barn.mapNotNull { it.annenForelder?.person?.fødselsnummer }

        return barn.flatMap { it.value.forelderBarnRelasjon }
                .filter { it.relatertPersonsIdent != søknad.fødselsnummer && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .plus(barneforeldreFraSøknad)
                .distinct()
                .let { pdlClient.hentAndreForeldre(it) }
    }


    private fun hentNavnForRelatertVedSivilstand(pdlSøker: PdlSøker): List<SivilstandMedNavn> {
        val mapPersonIdentTilNavn = pdlSøker.sivilstand.mapNotNull { it.relatertVedSivilstand }
                .distinct()
                .let { pdlClient.hentPersonKortBolk(it) }

        return pdlSøker.sivilstand.map {
            SivilstandMedNavn(type = Sivilstandstype.valueOf(it.type.name),
                              gyldigFraOgMed = it.gyldigFraOgMed,
                              relatertVedSivilstand = it.relatertVedSivilstand,
                              bekreftelsesdato = it.bekreftelsesdato,
                              metadata = it.metadata,
                              navn = mapPersonIdentTilNavn[it.relatertVedSivilstand]?.navn?.gjeldende()?.visningsnavn())
        }
    }


}