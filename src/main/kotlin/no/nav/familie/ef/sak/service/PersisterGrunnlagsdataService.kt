package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
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
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class PersisterGrunnlagsdataService(private val pdlClient: PdlClient,
                                    private val behandlingService: BehandlingService,
                                    private val grunnlagsdataRepository: GrunnlagsdataRepository,
                                    private val søknadService: SøknadService,
                                    private val featureToggleService: FeatureToggleService,
                                    private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    val logger = LoggerFactory.getLogger(this.javaClass)
    val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun populerGrunnlagsdataTabell() {
        val context = MDC.getCopyOfContextMap()
        Thread().runCatching {
            MDC.setContextMap(context)
            grunnlagsdataRepository
                    .finnBehandlingerSomManglerGrunnlagsdata()
                    .forEach {
                        logger.info("Lagrer grunnlagsdata for $it")
                        try {
                            opprettGrunnlagsdata(it)
                        } catch (e: Exception) {
                            logger.warn("Feilet $it")
                            secureLogger.warn("Feilet $it", e)
                        }
                    }
        }.also {
            logger.info("Done")
            MDC.clear()
        }
    }

    fun opprettGrunnlagsdata(behandlingId: UUID) {
        val grunnlagsdata = hentGrunnlagsdataFraRegister(behandlingId)
        grunnlagsdataRepository.insert(Grunnlagsdata(behandlingId = behandlingId, data = grunnlagsdata))
    }

    fun hentGrunnlagsdata(behandlingId: UUID): GrunnlagsdataDomene {
        return when (featureToggleService.isEnabled(BRUK_NY_DATAMODELL_TOGGLE, false)) {
            true -> hentLagretGrunnlagsdata(behandlingId)
            false -> hentGrunnlagsdataFraRegister(behandlingId)
        }
    }

    private fun hentLagretGrunnlagsdata(behandlingId: UUID): GrunnlagsdataDomene {
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrNull(behandlingId)
        return when {
            grunnlagsdata?.data != null -> grunnlagsdata.data
            behandlingService.hentBehandling(behandlingId).type != BehandlingType.BLANKETT ->
                error("Behandlingen mangler grunnlagsdata, her har noe gått veldig galt. Ring Alexandra")
            else -> hentGrunnlagsdataFraRegister(behandlingId) // når det er en blankettbehandling som savner grunnlagsdata
        }
    }

    private fun hentGrunnlagsdataFraRegister(behandlingId: UUID): GrunnlagsdataDomene {
        val søknad = søknadService.hentOvergangsstønad(behandlingId)
        val personIdent = søknad.fødselsnummer
        val barneforeldreFraSøknad = søknad.barn.mapNotNull { it.annenForelder?.person?.fødselsnummer }
        return hentGrunnlagsdataFraRegister(personIdent, barneforeldreFraSøknad)
    }

    fun hentGrunnlagsdataFraRegister(personIdent: String,
                                     barneforeldreFraSøknad: List<String>): GrunnlagsdataDomene {
        val pdlSøker = pdlClient.hentSøker(personIdent)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(pdlBarn, personIdent, barneforeldreFraSøknad)
        val dataTilAndreIdenter = hentDataTilAndreIdenter(pdlSøker)

        /*TODO VAD SKA VI BRUKE FRA MEDL ?? */
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)

        return GrunnlagsdataDomene(
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