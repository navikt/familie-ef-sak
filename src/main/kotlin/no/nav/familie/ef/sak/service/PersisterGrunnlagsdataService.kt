package no.nav.familie.ef.sak.service

import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.domene.Grunnlagsdata
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.mapper.GrunnlagsdataMapper.mapAnnenForelder
import no.nav.familie.ef.sak.mapper.GrunnlagsdataMapper.mapBarn
import no.nav.familie.ef.sak.mapper.GrunnlagsdataMapper.mapSøker
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.domain.Grunnlagdata
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class PersisterGrunnlagsdataService(private val pdlClient: PdlClient,
                                    private val grunnlagsdataRepository: GrunnlagsdataRepository,
                                    private val søknadService: SøknadService,
                                    private val featureToggleService: FeatureToggleService,
                                    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {
    /*TODO HVOR MANGE BEHANDLINGER FINNES DET? BORDE VI BRUKERE CHUNK og bygge pdlquery for varje chunk*/
    fun populerGrunnlagsdataTabell() {
        runBlocking {
            grunnlagsdataRepository
                    .finnBehandlingerSomManglerGrunnlagsdata()
                    .forEach { lagreGrunnlagsdata(it)}
        }

    }

    fun lagreGrunnlagsdata(behandlingId: UUID) {
        val søknad = søknadService.hentOvergangsstønad(behandlingId)
        val personIdent = søknad.fødselsnummer
        val barneforeldreFraSøknad = søknad.barn.mapNotNull { it.annenForelder?.person?.fødselsnummer }
        val grunnlagsdata = hentGrunnlagsdata(personIdent, barneforeldreFraSøknad)
        grunnlagsdataRepository.insert(Grunnlagdata(behandlingId = behandlingId, data = grunnlagsdata))
    }

    fun hentGrunnlagsdata(behandlingId: UUID): Grunnlagsdata {
        return when (featureToggleService.isEnabled(BRUK_NY_DATAMODELL_TOGGLE, false)) {
            true -> grunnlagsdataRepository.findByIdOrThrow(behandlingId).data
            false -> {
                val søknad = søknadService.hentOvergangsstønad(behandlingId)
                val personIdent = søknad.fødselsnummer
                val barneforeldreFraSøknad = søknad.barn.mapNotNull { it.annenForelder?.person?.fødselsnummer }
                hentGrunnlagsdata(personIdent, barneforeldreFraSøknad)
            }
        }

    }

    fun hentGrunnlagsdata(personIdent: String,
                          barneforeldreFraSøknad: List<String>): Grunnlagsdata {
        val pdlSøker = pdlClient.hentSøker(personIdent)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(pdlBarn, personIdent, barneforeldreFraSøknad)
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

    private fun hentPdlBarneForeldre(barn: Map<String, PdlBarn>,
                                     personIdent: String,
                                     barneforeldrePersonIdentFraSøknad: List<String>): Map<String, PdlAnnenForelder> {
        return barn.flatMap { it.value.forelderBarnRelasjon }
                .filter { it.relatertPersonsIdent != personIdent && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .plus(barneforeldrePersonIdentFraSøknad)
                .distinct()
                .let { pdlClient.hentAndreForeldre(it) }
    }

    private fun hentDataTilAndreIdenter(pdlSøker: PdlSøker): Map<String, PdlPersonKort> {
        val andreIdenter = pdlSøker.sivilstand.mapNotNull { it.relatertVedSivilstand } +
                           pdlSøker.fullmakt.map { it.motpartsPersonident }
        if (andreIdenter.isEmpty()) return emptyMap()
        return pdlClient.hentPersonKortBolk(andreIdenter)
    }

    companion object {

        const val BRUK_NY_DATAMODELL_TOGGLE = "familie.ef.sak.grunnlagsdataV2"

    }


}