package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.infrastruktur.config.getValue
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.egenansatt.EgenAnsattClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.EndringerIPersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.UtledEndringerUtil.finnEndringerIPerioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.UtledEndringerUtil.finnEndringerIPersonopplysninger
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class PersonopplysningerService(
    private val personService: PersonService,
    private val behandlingService: BehandlingService,
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val personopplysningerMapper: PersonopplysningerMapper,
    private val egenAnsattClient: EgenAnsattClient,
    @Qualifier("shortCache")
    private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentPersonopplysningerForBehandling(behandlingId: UUID): PersonopplysningerDto {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val søkerIdenter = personService.hentPersonIdenter(personIdent)
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val egenAnsatt = egenAnsatt(personIdent)

        return personopplysningerMapper.tilPersonopplysninger(
            grunnlagsdata,
            egenAnsatt,
            søkerIdenter,
        )
    }

    fun finnEndringerIPersonopplysningerForBehandling(
        behandling: Saksbehandling,
        tidligereGrunnlagsdata: GrunnlagsdataMedMetadata,
        nyGrunnlagsdata: GrunnlagsdataMedMetadata,
    ): EndringerIPersonopplysningerDto {
        val personIdent = behandling.ident
        val egenAnsatt = egenAnsatt(personIdent)
        val søkerIdenter = personService.hentPersonIdenter(personIdent)
        val tidligerePersonopplysninger =
            personopplysningerMapper.tilPersonopplysninger(
                tidligereGrunnlagsdata,
                egenAnsatt,
                søkerIdenter,
            )

        val nyePersonopplysninger =
            personopplysningerMapper.tilPersonopplysninger(
                nyGrunnlagsdata,
                egenAnsatt,
                søkerIdenter,
            )
        val endringer = finnEndringerIPersonopplysninger(tidligerePersonopplysninger, nyePersonopplysninger)
        val periodeEndringer = finnEndringerIPerioder(tidligereGrunnlagsdata, nyGrunnlagsdata)

        return EndringerIPersonopplysningerDto(LocalDateTime.now(), endringer.copy(perioder = periodeEndringer))
    }

    @Cacheable("personopplysninger", cacheManager = "shortCache")
    fun hentPersonopplysningerFraRegister(personIdent: String): PersonopplysningerDto {
        val personopplysningerGrunnlagsdataFraPdl = grunnlagsdataService.hentPersonopplysninger(personIdent)
        val egenAnsatt = egenAnsatt(personIdent)
        val identerFraPdl = personService.hentPersonIdenter(personIdent)

        return personopplysningerMapper.tilPersonopplysninger(
            personopplysninger = personopplysningerGrunnlagsdataFraPdl,
            grunnlagsdataOpprettet = LocalDateTime.now(),
            egenAnsatt = egenAnsatt,
            søkerIdenter = identerFraPdl,
        )
    }

    private fun egenAnsatt(personIdent: String) =
        cacheManager.getValue("egenAnsatt", personIdent) {
            egenAnsattClient.egenAnsatt(personIdent)
        }

    fun hentGjeldeneNavn(identer: List<String>): Map<String, String> {
        if (identer.isEmpty()) return emptyMap()
        logger.info("Henter navn til {} personer", identer.size)
        return personService
            .hentPersonKortBolk(identer)
            .map {
                it.key to
                    it.value.navn
                        .gjeldende()
                        .visningsnavn()
            }.toMap()
    }

    @Cacheable("navKontor")
    fun hentNavKontor(ident: String): NavKontorEnhet? = personopplysningerIntegrasjonerClient.hentNavKontor(ident)

    fun hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent: String): ADRESSEBESKYTTELSEGRADERING = personopplysningerIntegrasjonerClient.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(personIdent)
}
