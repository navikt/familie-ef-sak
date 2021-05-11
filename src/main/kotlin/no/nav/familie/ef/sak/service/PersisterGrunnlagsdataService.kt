package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.domene.Grunnlagsdata
import no.nav.familie.ef.sak.domene.tilPdlAnnenForelder
import no.nav.familie.ef.sak.domene.tilPdlBarn
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.mapper.AktivitetMapper
import no.nav.familie.ef.sak.mapper.BarnMedSamværMapper
import no.nav.familie.ef.sak.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.mapper.GrunnlagsdataMapper.mapAnnenForelder
import no.nav.familie.ef.sak.mapper.GrunnlagsdataMapper.mapBarn
import no.nav.familie.ef.sak.mapper.GrunnlagsdataMapper.mapSøker
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

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskap,
                                                 medlUnntak = grunnlagsdata.medlUnntak,
                                                 pdlSøker = grunnlagsdata.søker)
        val sivilstand = SivilstandMapper.tilDto(grunnlagsdata, søknad)
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
        val dataTilAndreIdenter = hentDataTilAndreIdenter(pdlSøker)

        /*TODO VAD SKA VI BRUKE FRA MEDL ?? */
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)

        return Grunnlagsdata(
                søker = mapSøker(pdlSøker, dataTilAndreIdenter),
                annenForelder = mapAnnenForelder(barneForeldre),
                medlUnntak = medlUnntak,
                barn = mapBarn(pdlBarn)
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

    private fun hentDataTilAndreIdenter(pdlSøker: PdlSøker): Map<String, PdlPersonKort> {
        val andreIdenter = pdlSøker.sivilstand.mapNotNull { it.relatertVedSivilstand } +
                           pdlSøker.fullmakt.map { it.motpartsPersonident }
        if (andreIdenter.isEmpty()) return emptyMap()
        return pdlClient.hentPersonKortBolk(andreIdenter)
    }


}