package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.InngangsvilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.MedlemskapDto
import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.mapper.BosituasjonMapper
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SivilstandMapper
import no.nav.familie.ef.sak.repository.RegistergrunnlagRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import no.nav.familie.ef.sak.repository.domain.RegistergrunnlagData
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
class GrunnlagsdataService(private val registergrunnlagRepository: RegistergrunnlagRepository,
                           private val pdlClient: PdlClient,
                           private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                           private val medlemskapMapper: MedlemskapMapper,
                           private val behandlingService: BehandlingService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrunnlag(behandlingId: UUID,
                     søknad: SøknadsskjemaOvergangsstønad): InngangsvilkårGrunnlagDto {
        val registergrunnlag = hentEllerOpprettRegistergrunnlag(søknad.fødselsnummer, behandlingId)
        val registergrunnlagData = registergrunnlag.endringer ?: registergrunnlag.data
        val medlemskapSøknadsgrunnlag = medlemskapMapper.mapSøknadsgrunnlag(medlemskapsdetaljer = søknad.medlemskap)
        val medlemskap = MedlemskapDto(søknadsgrunnlag = medlemskapSøknadsgrunnlag,
                                       registergrunnlag = registergrunnlagData.medlemskap)

        val sivilstandSøknadsgrunnlag = SivilstandMapper.mapSøknadsgrunnlag(sivilstandsdetaljer = søknad.sivilstand)
        val sivilstand = SivilstandInngangsvilkårDto(søknadsgrunnlag = sivilstandSøknadsgrunnlag,
                                                     registergrunnlag = registergrunnlagData.sivilstand)

        return InngangsvilkårGrunnlagDto(medlemskap = medlemskap,
                                         sivilstand = sivilstand,
                                         bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon),
                                         endringer = finnEndringerIRegistergrunnlag(registergrunnlag)
        )
    }

    fun godkjennEndringerIRegistergrunnlag(behandlingId: UUID) {
        val eksisterendeRegistergrunnlag = registergrunnlagRepository.findByIdOrThrow(behandlingId)
        if (eksisterendeRegistergrunnlag.endringer == null) {
            logger.warn("Prøver å godkjenne endringer i registergrunnlag uten att det finnes endringer behandling=$behandlingId")
            return
        }
        logger.info("Godkjenner endringer i registergrunnlag behandling=$behandlingId")
        godkjennOgSjekkForNyeEndringer(behandlingId, eksisterendeRegistergrunnlag)
    }

    fun sjekkForEndringerIRegistergrunnlag(behandling: Behandling): Boolean {
        val grunnlagsdata = registergrunnlagRepository.findByIdOrNull(behandling.id) ?: return false
        if (grunnlagsdata.sporbar.endret.endretTid.isBefore(LocalDateTime.now().minusHours(4))) {
            val søknad = behandlingService.hentOvergangsstønad(behandling.id)
            val grunnlagsdataData = opprettRegistergrunnlag(søknad.fødselsnummer)
            val diff = grunnlagsdata.data == grunnlagsdataData
            if (diff != grunnlagsdata.diff) {
                logger.info("Oppdaterer registergrunnlag behandling=${behandling.id} steg=${behandling.steg} med diff=$diff")
                registergrunnlagRepository.update(grunnlagsdata.copy(endringer = grunnlagsdataData,
                                                                     diff = diff))
            }
            return diff
        } else {
            return grunnlagsdata.diff
        }
    }

    private fun hentEllerOpprettRegistergrunnlag(personIdent: String, behandlingId: UUID): Registergrunnlag {
        var grunnlagsdata = registergrunnlagRepository.findByIdOrNull(behandlingId)
        if (grunnlagsdata == null) {
            logger.debug("Oppretter registergrunnlag for behandling=$behandlingId")
            grunnlagsdata = Registergrunnlag(behandlingId = behandlingId, data = opprettRegistergrunnlag(personIdent))
            registergrunnlagRepository.insert(grunnlagsdata)
        }
        return grunnlagsdata
    }

    private fun godkjennOgSjekkForNyeEndringer(behandlingId: UUID,
                                               eksisterendeRegistergrunnlag: Registergrunnlag) {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val registergrunnlag = opprettRegistergrunnlag(søknad.fødselsnummer)
        requireNotNull(eksisterendeRegistergrunnlag.endringer) { "Endringer kan ikke være null - behandling=$behandlingId" }
        if (registergrunnlag != eksisterendeRegistergrunnlag.endringer) {
            logger.warn("Godkjenner nye endringer i registergrunnlag, men har nye endringer behandling=$behandlingId")
            registergrunnlagRepository.update(eksisterendeRegistergrunnlag.copy(data = eksisterendeRegistergrunnlag.endringer,
                                                                                endringer = registergrunnlag,
                                                                                diff = true))
        } else {
            registergrunnlagRepository.update(eksisterendeRegistergrunnlag.copy(data = eksisterendeRegistergrunnlag.endringer,
                                                                                endringer = null,
                                                                                diff = false))
        }
    }

    private fun opprettRegistergrunnlag(personIdent: String): RegistergrunnlagData {
        val pdlSøker = pdlClient.hentSøker(personIdent)
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)
        return RegistergrunnlagData(medlemskap = medlemskapMapper.mapRegistergrunnlag(pdlSøker, medlUnntak),
                                    sivilstand = SivilstandMapper.mapRegistergrunnlag(pdlSøker))
    }

    private fun finnEndringerIRegistergrunnlag(registergrunnlag: Registergrunnlag): Map<String, Map<String, Boolean>> {
        val endringer = registergrunnlag.endringer ?: return emptyMap()
        val data = registergrunnlag.data
        return RegistergrunnlagData::class.memberProperties.map {
            it.name to diff(it.getter.call(data) as Any,
                            it.getter.call(endringer) as Any,
                            it.returnType.classifier as KClass<*>)
        }.toMap()
    }

    private fun diff(value1: Any, value2: Any, kClass: KClass<*>): Map<String, Boolean> =
            kClass.memberProperties.map {
                it.name to (it.getter.call(value1) != it.getter.call(value2))
            }.toMap()

}