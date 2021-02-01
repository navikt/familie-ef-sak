package no.nav.familie.ef.sak.service

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
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@Service
class GrunnlagsdataService(private val grunnlagsdataRepository: GrunnlagsdataRepository,
                           private val behandlingService: BehandlingService,
                           private val vurderingService: VurderingService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

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

    fun harDiffIGrunnlagsdata(behandling: Behandling): Boolean {
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        return if (grunnlagsdata.sporbar.endret.endretTid.isBefore(LocalDateTime.now().minusHours(4))) {
            beregnOgLagreNyDiff(behandling, grunnlagsdata)
        } else {
            grunnlagsdata.diff
        }

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
        val grunnlag = vurderingService.hentGrunnlag(søknad.fødselsnummer, søknad)
        return GrunnlagsdataData(grunnlag.medlemskap.registergrunnlag,
                                 grunnlag.sivilstand.registergrunnlag)
    }

    private fun diffGrunnlagsdata(value1: GrunnlagsdataData, value2: GrunnlagsdataData): Map<String, Map<String, Boolean>> {
        fun diff(value1: Any, value2: Any, kClass: KClass<*>): Map<String, Boolean> =
                kClass.memberProperties.map {
                    it.name to (it.getter.call(value1) == it.getter.call(value2))
                }.toMap()
        return GrunnlagsdataData::class.memberProperties.map {
            it.name to diff(it.getter.call(value1) as Any,
                            it.getter.call(value2) as Any,
                            it.returnType.classifier as KClass<*>)
        }.toMap()
    }

}