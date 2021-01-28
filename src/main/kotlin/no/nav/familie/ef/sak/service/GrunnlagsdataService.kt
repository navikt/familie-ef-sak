package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import no.nav.familie.ef.sak.repository.domain.GrunnlagsdataData
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class GrunnlagsdataService(private val grunnlagsdataRepository: GrunnlagsdataRepository,
                           private val vurderingService: VurderingService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun opprettGrunnlagsdataEllerDiff(behandling: Behandling,
                                      søknad: SøknadsskjemaOvergangsstønad) {
        val grunnlagsdataData = lagGrunnlagsdataData(søknad)
        val eksisterendeGrunnlagsdata = grunnlagsdataRepository.findByIdOrNull(behandling.id)
        if (eksisterendeGrunnlagsdata == null) {
            grunnlagsdataRepository.insert(Grunnlagsdata(behandlingId = behandling.id,
                                                         data = grunnlagsdataData))
        } else {
            val diff = eksisterendeGrunnlagsdata.data == grunnlagsdataData
            if (diff != eksisterendeGrunnlagsdata.diff) {
                logger.info("Oppdaterer behandling=${behandling.id} med diff=$diff")
                grunnlagsdataRepository.update(eksisterendeGrunnlagsdata.copy(diff = diff))
            }
        }
    }

    fun oppdaterGrunnlagsdata(behandling: Behandling,
                              søknad: SøknadsskjemaOvergangsstønad) {
        if (behandling.status != BehandlingStatus.UTREDES) {
            val message = "Prøver å oppdatere grunnlagsdata på en behandling som ikke utredes"
            throw Feil(message = "$message - behandling=${behandling.id}",
                       frontendFeilmelding = message)
        }
        val grunnlagsdataData = lagGrunnlagsdataData(søknad)
        val eksisterendeGrunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        if (eksisterendeGrunnlagsdata.data == grunnlagsdataData) {
            throw Feil(message = "Ingen forskjell i grunnlagsdata",
                       frontendFeilmelding = "Ingen forskjell i grunnlagsdata")
        }
        grunnlagsdataRepository.update(eksisterendeGrunnlagsdata.copy(data = grunnlagsdataData))
    }

    private fun lagGrunnlagsdataData(søknad: SøknadsskjemaOvergangsstønad): GrunnlagsdataData {
        val grunnlag = vurderingService.hentGrunnlag(søknad.fødselsnummer, søknad)
        return GrunnlagsdataData(grunnlag.medlemskap.registergrunnlag,
                                 grunnlag.sivilstand.registergrunnlag)
    }

    fun harDiffIGrunnlagsdata(behandling: Behandling): Boolean {
        return grunnlagsdataRepository.harDiffIGrunnlagsdata(behandling.id)
    }

}