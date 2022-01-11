package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.config.getValue
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class PersonopplysningerService(private val personService: PersonService,
                                private val behandlingService: BehandlingService,
                                private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
                                private val grunnlagsdataService: GrunnlagsdataService,
                                private val personopplysningerMapper: PersonopplysningerMapper,
                                @Qualifier("shortCache")
                                private val cacheManager: CacheManager) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentPersonopplysninger(behandlingId: UUID): PersonopplysningerDto {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val søkerIdenter = personService.hentPersonIdenter(personIdent)
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val egenAnsatt = egenAnsatt(personIdent)

        return personopplysningerMapper.tilPersonopplysninger(
                grunnlagsdata,
                egenAnsatt,
                søkerIdenter
        )
    }

    @Cacheable("personopplysninger", cacheManager = "shortCache")
    fun hentPersonopplysninger(personIdent: String): PersonopplysningerDto {
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdataFraRegister(personIdent, emptyList())
        val egenAnsatt = egenAnsatt(personIdent)
        val identerFraPdl = personService.hentPersonIdenter(personIdent)

        return personopplysningerMapper.tilPersonopplysninger(
                GrunnlagsdataMedMetadata(grunnlagsdata,
                                         lagtTilEtterFerdigstilling = false,
                                         opprettetTidspunkt = LocalDateTime.now()),
                egenAnsatt,
                identerFraPdl
        )
    }

    private fun egenAnsatt(personIdent: String) = cacheManager.getValue("egenAnsatt", personIdent) {
        personopplysningerIntegrasjonerClient.egenAnsatt(personIdent)
    }

    fun hentGjeldeneNavn(identer: List<String>): Map<String, String> {
        if (identer.isEmpty()) return emptyMap()
        logger.info("Henter navn til {} personer", identer.size)
        return personService.hentPdlPersonKort(identer).map { it.key to it.value.navn.gjeldende().visningsnavn() }.toMap()
    }

    @Cacheable("navKontor")
    fun hentNavKontor(ident: String): NavKontorEnhet {
        return personopplysningerIntegrasjonerClient.hentNavKontor(ident)
    }
}

