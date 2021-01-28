package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import no.nav.familie.ef.sak.repository.domain.GrunnlagsdataData
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class GrunnlagsdataService(private val grunnlagsdataRepository: GrunnlagsdataRepository,
                           private val behandlingService: BehandlingService,
                           private val vurderingService: VurderingService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun oppdaterGrunnlagsdata(behandling: Behandling) {
        val søknad = behandlingService.hentOvergangsstønad(behandling.id)
        val grunnlagsdataData = lagGrunnlagsdataData(søknad)
        val eksisterendeGrunnlagsdata = grunnlagsdataRepository.findByIdOrNull(behandling.id)
        if (eksisterendeGrunnlagsdata == null) {
            grunnlagsdataRepository.insert(Grunnlagsdata(behandlingId = behandling.id,
                                                         data = grunnlagsdataData))
        } else {
            val diff = eksisterendeGrunnlagsdata.data == grunnlagsdataData
            if (diff != eksisterendeGrunnlagsdata.diff) {
                logger.info("Oppdaterer behandling=${behandling.id} steg=${behandling.steg} med diff=$diff")
                grunnlagsdataRepository.update(eksisterendeGrunnlagsdata.copy(data = grunnlagsdataData,
                                                                              //tidligereData = eksisterendeGrunnlagsdata.data,
                                                                              diff = diff))
            }
        }
    }

    // TODO inngangsvilkår -> godkjenn grunnlagsdataendringer?

    /*
    fun oppdaterGrunnlagsdata(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != BehandlingStatus.UTREDES) {
            val message = "Prøver å oppdatere grunnlagsdata på en behandling som ikke utredes"
            throw Feil(message = "$message - behandling=${behandling.id}",
                       frontendFeilmelding = message)
        }
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val grunnlagsdataData = lagGrunnlagsdataData(søknad)
        val eksisterendeGrunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        if (eksisterendeGrunnlagsdata.data == grunnlagsdataData) {
            throw Feil(message = "Ingen forskjell i grunnlagsdata",
                       frontendFeilmelding = "Ingen forskjell i grunnlagsdata")
        }
        grunnlagsdataRepository.update(eksisterendeGrunnlagsdata.copy(data = grunnlagsdataData,
                                                                      tidligereData = eksisterendeGrunnlagsdata.data,
                                                                      diff = true))
    }*/

    fun harDiffIGrunnlagsdata(behandling: Behandling): Boolean {
        return grunnlagsdataRepository.harDiffIGrunnlagsdata(behandling.id) ?: false
    }

    private fun lagGrunnlagsdataData(søknad: SøknadsskjemaOvergangsstønad): GrunnlagsdataData {
        val grunnlag = vurderingService.hentGrunnlag(søknad.fødselsnummer, søknad)
        return GrunnlagsdataData(grunnlag.medlemskap.registergrunnlag,
                                 grunnlag.sivilstand.registergrunnlag)
    }

    fun diffGrunnlagsdata(newValue: GrunnlagsdataData, oldValue: GrunnlagsdataData): Map<String, Map<String, GrunnlagStatus>> {
        val newValues = objectMapper.convertValue<Map<String, Map<String, Any?>>>(newValue)
        val oldValues = objectMapper.convertValue<Map<String, Map<String, Any?>>>(oldValue)
        if (newValues.size != oldValues.size || !newValues.keys.containsAll(oldValues.keys)) {
            error("Forskjell i antall felt/nullable felt i Grunnlagsdata," +
                  " hvis denne feiler finnes det nullable felt i GrunnlagsdataData og diff må forbedres")
        }
        return newValues.map { (k, v) ->
            k to diff(v, oldValues[k]!!)
        }.toMap()
    }

    enum class GrunnlagStatus {
        NY,
        SAVNES,
        ENDRET,
        UENDRET
    }

    private fun diff(newValues: Map<String, Any?>, oldValues: Map<String, Any?>): Map<String, GrunnlagStatus> {
        val values = newValues.map { (name, newValue) ->
            val oldValue = oldValues[name]
            val status = when {
                oldValue == newValue -> GrunnlagStatus.UENDRET
                oldValue == null -> GrunnlagStatus.NY
                newValue == null -> GrunnlagStatus.SAVNES
                else -> GrunnlagStatus.ENDRET
            }
            name to status
        }.toMap()
        return if (newValues.keys.containsAll(oldValues.keys)) {
            values
        } else {
            val verdierSomSavnes = oldValues.filterNot { newValues.containsKey(it.key) }
                    .map { (name, _) -> name to GrunnlagStatus.SAVNES }
                    .toMap()
            values + verdierSomSavnes
        }
    }

}