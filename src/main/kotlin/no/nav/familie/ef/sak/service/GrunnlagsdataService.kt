package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.api.dto.MedlemskapDto
import no.nav.familie.ef.sak.api.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.mapper.*
import no.nav.familie.ef.sak.repository.RegistergrunnlagRepository
import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import no.nav.familie.ef.sak.repository.domain.RegistergrunnlagData
import no.nav.familie.ef.sak.repository.domain.Registergrunnlagsendringer
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
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
                     søknad: SøknadsskjemaOvergangsstønad): VilkårGrunnlagDto {
        val registergrunnlag = registergrunnlagRepository.findByIdOrThrow(behandlingId)
        val registergrunnlagData = registergrunnlag.endringer ?: registergrunnlag.data

        val medlemskapSøknadsgrunnlag = medlemskapMapper.mapSøknadsgrunnlag(medlemskapsdetaljer = søknad.medlemskap)
        val medlemskap = MedlemskapDto(søknadsgrunnlag = medlemskapSøknadsgrunnlag,
                                       registergrunnlag = registergrunnlagData.medlemskap)

        val sivilstandSøknadsgrunnlag = SivilstandMapper.mapSøknadsgrunnlag(sivilstandsdetaljer = søknad.sivilstand)
        val sivilstand = SivilstandInngangsvilkårDto(søknadsgrunnlag = sivilstandSøknadsgrunnlag,
                                                     registergrunnlag = registergrunnlagData.sivilstand)
        val sivilstandsplaner = SivilstandsplanerMapper.tilDto(sivilstandsplaner = søknad.sivilstandsplaner)
        val barnMedSamvær = BarnMedSamværMapper.slåSammenBarnMedSamvær(BarnMedSamværMapper.mapSøknadsgrunnlag(søknad.barn),
                                                                       registergrunnlagData.barnMedSamvær).sortedByDescending {
            it.registergrunnlag.fødselsnummer?.let { fødsesnummer -> Fødselsnummer(fødsesnummer).fødselsdato }
            ?: it.søknadsgrunnlag.fødselTermindato
        }

        val sagtOppEllerRedusertStilling = SagtOppEllerRedusertStillingMapper.tilDto(situasjon = søknad.situasjon)

        val aktivitet = AktivitetMapper.tilDto(aktivitet = søknad.aktivitet, situasjon = søknad.situasjon, barn = søknad.barn )
        return VilkårGrunnlagDto(medlemskap = medlemskap,
                                 sivilstand = sivilstand,
                                 bosituasjon = BosituasjonMapper.tilDto(søknad.bosituasjon),
                                 barnMedSamvær = barnMedSamvær,
                                 sivilstandsplaner = sivilstandsplaner,
                                 aktivitet = aktivitet,
                                 sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling)
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
            logger.debug("Oppretter registergrunnlag for behandling=$behandlingId")
            registergrunnlag = Registergrunnlag(behandlingId = behandlingId, data = hentRegistergrunnlag(behandlingId))
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
        val grunnlagsdataData = hentRegistergrunnlag(behandlingId)
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
        val nyttRegistergrunnlag = hentRegistergrunnlag(behandlingId)
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

    private fun hentRegistergrunnlag(behandlingId: UUID): RegistergrunnlagData {
        val søknad = behandlingService.hentOvergangsstønad(behandlingId)
        val personIdent = søknad.fødselsnummer

        val pdlSøker = pdlClient.hentSøker(personIdent)
        val pdlBarn = hentPdlBarn(pdlSøker)
        val barneForeldre = hentPdlBarneForeldre(søknad, pdlBarn)
        val medlUnntak = familieIntegrasjonerClient.hentMedlemskapsinfo(ident = personIdent)

        val barnMedSamvær = BarnMedSamværMapper.mapRegistergrunnlag(pdlBarn, barneForeldre, søknad, pdlSøker.bostedsadresse)
        return RegistergrunnlagData(medlemskap = medlemskapMapper.mapRegistergrunnlag(pdlSøker, medlUnntak),
                                    sivilstand = SivilstandMapper.mapRegistergrunnlag(pdlSøker),
                                    barnMedSamvær = barnMedSamvær)
    }

    private fun hentPdlBarneForeldre(søknad: SøknadsskjemaOvergangsstønad,
                                     barn: Map<String, PdlBarn>): Map<String, PdlAnnenForelder> {
        val barneforeldreFraSøknad =
                søknad.barn.mapNotNull {
                    it.annenForelder?.person?.fødselsnummer
                }

        val barneforeldre = barn.map { it.value.forelderBarnRelasjon }
                .flatten()
                .filter { it.relatertPersonsIdent != søknad.fødselsnummer && it.relatertPersonsRolle != Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .plus(barneforeldreFraSøknad)
                .distinct()
                .let { pdlClient.hentAndreForeldre(it) }
        return barneforeldre
    }

    private fun hentPdlBarn(pdlSøker: PdlSøker): Map<String, PdlBarn> {
        val barn = pdlSøker.forelderBarnRelasjon
                .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
                .map { it.relatertPersonsIdent }
                .let { pdlClient.hentBarn(it) }
                .filter { it.value.fødsel.gjeldende()?.fødselsdato != null }
                .filter { it.value.fødsel.first().fødselsdato!!.plusYears(18).isAfter(LocalDate.now()) }
        return barn
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