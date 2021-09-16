package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(private val personService: PersonService,
                                private val søknadService: SøknadService,
                                private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                                private val grunnlagsdataService: GrunnlagsdataService,
                                private val personopplysningerMapper: PersonopplysningerMapper) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentPersonopplysninger(behandlingId: UUID): PersonopplysningerDto {
        val søknad = søknadService.hentOvergangsstønad(behandlingId)
        val personIdent = søknad.fødselsnummer
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val egenAnsatt = familieIntegrasjonerClient.egenAnsatt(personIdent)


        return personopplysningerMapper.tilPersonopplysninger(
                grunnlagsdata,
                egenAnsatt,
                personIdent
        )
    }

    fun hentPersonopplysninger(personIdent: String): PersonopplysningerDto {
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdataFraRegister(personIdent, emptyList())
        val egenAnsatt = familieIntegrasjonerClient.egenAnsatt(personIdent)


        return personopplysningerMapper.tilPersonopplysninger(
                GrunnlagsdataMedMetadata(grunnlagsdata, lagtTilEtterFerdigstilling = false),
                egenAnsatt,
                personIdent
        )
    }

    fun hentGjeldeneNavn(identer: List<String>): Map<String, String> {
        if (identer.isEmpty()) return emptyMap()
        logger.info("Henter navn til {} personer", identer.size)
        return personService.hentPdlPersonKort(identer).map { it.key to it.value.navn.gjeldende().visningsnavn() }.toMap()
    }

    @Cacheable("navKontor")
    fun hentNavKontor(ident: String): NavKontorEnhet {
        return familieIntegrasjonerClient.hentNavKontor(ident)
    }
}
