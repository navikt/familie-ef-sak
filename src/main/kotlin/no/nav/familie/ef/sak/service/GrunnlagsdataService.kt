package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.InngangsvilkårGrunnlagDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SivilstandMapper
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import no.nav.familie.ef.sak.repository.domain.GrunnlagsdataData
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@Service
class GrunnlagsdataService(private val grunnlagsdataRepository: GrunnlagsdataRepository,
                           private val pdlClient: PdlClient,
                           private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                           private val medlemskapMapper: MedlemskapMapper,
                           private val behandlingService: BehandlingService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrunnlag(personIdent: String,
                     søknad: SøknadsskjemaOvergangsstønad): InngangsvilkårGrunnlagDto {
        val pdlSøker = pdlClient.hentSøker(personIdent)
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskap,
                                                 medlUnntak = medlUnntak,
                                                 pdlSøker = pdlSøker)

        val sivilstand = SivilstandMapper.tilDto(sivilstandsdetaljer = søknad.sivilstand,
                                                 pdlSøker = pdlSøker)
        val bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon)

        return InngangsvilkårGrunnlagDto(medlemskap, sivilstand, bosituasjon)
    }

    /**
     * Setter inn grunnlagsdata hvis det ikke eksisterer siden tidligere.
     * Hvis det eksisterer siden tidligere og det finnes endringer, så settes endringene til data
     */
    fun opprettEllerGodkjennGrunnlagsdata(behandling: Behandling) {
        val søknad = behandlingService.hentOvergangsstønad(behandling.id)
        val grunnlagsdataData = lagGrunnlagsdataData(søknad)
        val eksisterendeGrunnlagsdata = grunnlagsdataRepository.findByIdOrNull(behandling.id)
        if (eksisterendeGrunnlagsdata == null) {
            grunnlagsdataRepository.insert(Grunnlagsdata(behandlingId = behandling.id,
                                                         data = grunnlagsdataData))
        } else if (eksisterendeGrunnlagsdata.endringer != null) {
            grunnlagsdataRepository.update(eksisterendeGrunnlagsdata.copy(data = eksisterendeGrunnlagsdata.endringer,
                                                                          endringer = null,
                                                                          diff = false))
        }
    }

    fun sjekkOmDetErDiffIGrunnlagsdata(behandling: Behandling): Boolean {
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        return if (grunnlagsdata.sporbar.endret.endretTid.isBefore(LocalDateTime.now().minusHours(4))) {
            beregnOgLagreNyDiff(behandling, grunnlagsdata)
        } else {
            grunnlagsdataRepository.oppdaterEndretTid()
            grunnlagsdata.diff
        }

    }

    fun hentDiff(behandlingId: UUID): Map<String, Map<String, Boolean>> {
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandlingId)
        if(grunnlagsdata.endringer == null) {
            return emptyMap()
        }
        return diffGrunnlagsdata(grunnlagsdata.endringer, grunnlagsdata.data)
    }

    private fun beregnOgLagreNyDiff(behandling: Behandling, grunnlagsdata: Grunnlagsdata): Boolean {
        val søknad = behandlingService.hentOvergangsstønad(behandling.id)
        val grunnlagsdataData = lagGrunnlagsdataData(søknad)
        val diff = grunnlagsdata.data == grunnlagsdataData
        if (diff != grunnlagsdata.diff) {
            logger.info("Oppdaterer behandling=${behandling.id} steg=${behandling.steg} med diff=$diff")
            grunnlagsdataRepository.update(grunnlagsdata.copy(endringer = grunnlagsdataData,
                                                              diff = diff))
        }
        return diff
    }

    private fun lagGrunnlagsdataData(søknad: SøknadsskjemaOvergangsstønad): GrunnlagsdataData {
        val grunnlag = hentGrunnlag(søknad.fødselsnummer, søknad)
        return GrunnlagsdataData(grunnlag.medlemskap.registergrunnlag,
                                 grunnlag.sivilstand.registergrunnlag)
    }

    private fun diffGrunnlagsdata(value1: GrunnlagsdataData, value2: GrunnlagsdataData): Map<String, Map<String, Boolean>> {
        fun diff(value1: Any, value2: Any, kClass: KClass<*>): Map<String, Boolean> =
                kClass.memberProperties.map {
                    it.name to (it.getter.call(value1) != it.getter.call(value2))
                }.toMap()
        return GrunnlagsdataData::class.memberProperties.map {
            it.name to diff(it.getter.call(value1) as Any,
                            it.getter.call(value2) as Any,
                            it.returnType.classifier as KClass<*>)
        }.toMap()
    }

}