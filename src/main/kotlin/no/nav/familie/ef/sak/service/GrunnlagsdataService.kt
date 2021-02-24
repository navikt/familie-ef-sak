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
import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import no.nav.familie.ef.sak.repository.domain.RegistergrunnlagData
import no.nav.familie.ef.sak.repository.domain.Registergrunnlagsendringer
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Denne klassen håndterer henting, persistering og oppdatering av grunnlagsdata
 *
 * Når man henter grunnlagsdata første gangen så legges disse inn i databasen.
 * Hvis det har gått mer enn 4h fra att man henter dataen sist så sjekker man om det er en diff i grunnlagsdataen
 *  Hvis det finnes endringer legges disse inn i feltet endringer.
 *
 * Saksbehandler må godkjenne de nye endringene for att de skal bli lagret i data i [Registergrunnlag]
 */
@Service
class GrunnlagsdataService(private val registergrunnlagRepository: RegistergrunnlagRepository,
                           private val pdlClient: PdlClient,
                           private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                           private val medlemskapMapper: MedlemskapMapper,
                           private val behandlingService: BehandlingService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentGrunnlag(behandlingId: UUID,
                     søknad: SøknadsskjemaOvergangsstønad): InngangsvilkårGrunnlagDto {
        val registergrunnlag = registergrunnlagRepository.findByIdOrThrow(behandlingId)
        val registergrunnlagData = registergrunnlag.endringer ?: registergrunnlag.data

        val medlemskapSøknadsgrunnlag = medlemskapMapper.mapSøknadsgrunnlag(medlemskapsdetaljer = søknad.medlemskap)
        val medlemskap = MedlemskapDto(søknadsgrunnlag = medlemskapSøknadsgrunnlag,
                                       registergrunnlag = registergrunnlagData.medlemskap)

        val sivilstandSøknadsgrunnlag = SivilstandMapper.mapSøknadsgrunnlag(sivilstandsdetaljer = søknad.sivilstand)
        val sivilstand = SivilstandInngangsvilkårDto(søknadsgrunnlag = sivilstandSøknadsgrunnlag,
                                                     registergrunnlag = registergrunnlagData.sivilstand)

        return InngangsvilkårGrunnlagDto(medlemskap = medlemskap,
                                         sivilstand = sivilstand,
                                         bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon))
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

    fun hentEndringerIRegistergrunnlag(behandlingId: UUID): Registergrunnlagsendringer {
        val registergrunnlag = hentEllerOpprettRegistergrunnlag(behandlingId)
        return finnEndringerIRegistergrunnlag(registergrunnlag)
    }

    private fun hentEllerOpprettRegistergrunnlag(behandlingId: UUID): Registergrunnlag {
        var registergrunnlag = registergrunnlagRepository.findByIdOrNull(behandlingId)
        if (registergrunnlag == null) {
            val personIdent =
                    behandlingService.hentOvergangsstønad(behandlingId).fødselsnummer // TODO hente aktivident fra fagsak?
            logger.debug("Oppretter registergrunnlag for behandling=$behandlingId")

            registergrunnlag = Registergrunnlag(behandlingId = behandlingId, data = hentRegistergrunnlag(personIdent))
            return registergrunnlagRepository.insert(registergrunnlag)
        } else if (registergrunnlag.sporbar.endret.endretTid.isBefore(LocalDateTime.now().minusHours(4))) {
            return oppdaterRegistergrunnlag(registergrunnlag)
        } else {
            return registergrunnlag
        }
    }

    /**
     * Spesielle caser:
     *
     * Case 1 - Saksbehandler godkjenner ikke ny grunnlagsdata, men går inn på saken senere igjen, hvor det er nye endringer på nytt
     * Kl 00 - saksbehandler går inn på saker og det er en diff i dataen, NY != opprinnelig
     *  data=opprinnelig, endringer = NY
     * Kl 04 - saksbehandler går inn på nytt og det er ny diff, NY2 !=opprinnelig && NY2 != NY
     *  data=opprinnelig, endringer = NY2
     *
     * Case 2 - Saksbehandler godkjenner ikke ny grunnlagsdata, når man går inn senere så er det en ny diff mot forrige gang, men som ikke er en diff mot opprinnelig data
     * Kl 00 - saksbehandler går inn på saker og det er en diff i dataen NY != opprinnelig
     *  data=opprinnelig, endringer = NY
     * Kl 04 - saksbehandler går inn på nytt og det er ny diff, NY = opprinnelig
     *  data=opprinnelig, endringer = null
     */
    private fun oppdaterRegistergrunnlag(grunnlagsdata: Registergrunnlag): Registergrunnlag {
        val behandlingId = grunnlagsdata.behandlingId
        val søknad = behandlingService.hentOvergangsstønad(behandlingId) // TODO hente aktivident fra fagsak?
        val grunnlagsdataData = hentRegistergrunnlag(søknad.fødselsnummer)
        val diff = grunnlagsdata.data != grunnlagsdataData

        return if (diff) {
            logger.info("Oppdaterer registergrunnlag behandling=$behandlingId med diff=$diff")
            registergrunnlagRepository.update(grunnlagsdata.copy(endringer = grunnlagsdataData))
        } else if (grunnlagsdata.endringer != null) {
            logger.info("Fjerner endringer i registergrunnlag behandling=$behandlingId med diff=$diff")
            registergrunnlagRepository.update(grunnlagsdata.copy(endringer = null))
        } else {
            logger.debug("Trigger oppdatering av endretTid registergrunnlag behandling=$behandlingId")
            registergrunnlagRepository.update(grunnlagsdata)
        }
    }

    private fun godkjennOgSjekkForNyeEndringer(behandlingId: UUID,
                                               eksisterendeRegistergrunnlag: Registergrunnlag) {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val nyttRegistergrunnlag = hentRegistergrunnlag(søknad.fødselsnummer)
        requireNotNull(eksisterendeRegistergrunnlag.endringer) { "Endringer kan ikke være null - behandling=$behandlingId" }
        if (nyttRegistergrunnlag != eksisterendeRegistergrunnlag.endringer) {
            logger.warn("Godkjenner nye endringer i registergrunnlag, men har nye endringer behandling=$behandlingId")
            registergrunnlagRepository.update(eksisterendeRegistergrunnlag.copy(data = eksisterendeRegistergrunnlag.endringer,
                                                                                endringer = nyttRegistergrunnlag))
        } else {
            registergrunnlagRepository.update(eksisterendeRegistergrunnlag.copy(data = eksisterendeRegistergrunnlag.endringer,
                                                                                endringer = null))
        }
    }

    private fun hentRegistergrunnlag(personIdent: String): RegistergrunnlagData {
        val pdlSøker = pdlClient.hentSøker(personIdent)
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)
        return RegistergrunnlagData(medlemskap = medlemskapMapper.mapRegistergrunnlag(pdlSøker, medlUnntak),
                                    sivilstand = SivilstandMapper.mapRegistergrunnlag(pdlSøker))
    }

    private fun finnEndringerIRegistergrunnlag(registergrunnlag: Registergrunnlag): Registergrunnlagsendringer {
        val endringer = registergrunnlag.endringer ?: return emptyMap()
        val data = registergrunnlag.data
        return RegistergrunnlagData::class.memberProperties.map {
            it.name to diff(it.getter.call(data) as Any,
                            it.getter.call(endringer) as Any,
                            it.returnType.classifier as KClass<*>)
        }.toMap()
    }

    private fun diff(value1: Any, value2: Any, kClass: KClass<*>): List<String> =
            kClass.memberProperties
                    .filter { (it.getter.call(value1) != it.getter.call(value2)) }
                    .map { it.name }

}