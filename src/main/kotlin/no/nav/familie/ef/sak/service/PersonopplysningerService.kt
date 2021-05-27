package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.domene.GrunnlagsdataMedType
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.repository.domain.GrunnlagsdataType
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonopplysningerService(private val personService: PersonService,
                                private val søknadService: SøknadService,
                                private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                                private val persisterGrunnlagsdataService: PersisterGrunnlagsdataService,
                                private val personopplysningerMapper: PersonopplysningerMapper) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentPersonopplysninger(behandlingId: UUID): PersonopplysningerDto {
        val søknad = søknadService.hentOvergangsstønad(behandlingId)
        val personIdent = søknad.fødselsnummer
        val grunnlagsdata = persisterGrunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val egenAnsatt = familieIntegrasjonerClient.egenAnsatt(personIdent)


        return personopplysningerMapper.tilPersonopplysninger(
                grunnlagsdata,
                egenAnsatt,
                personIdent
        )
    }

    fun hentPersonopplysninger(personIdent: String): PersonopplysningerDto {
        val grunnlagsdata = persisterGrunnlagsdataService.hentGrunnlagsdataFraRegister(personIdent, emptyList())
        val egenAnsatt = familieIntegrasjonerClient.egenAnsatt(personIdent)


        return personopplysningerMapper.tilPersonopplysninger(
                GrunnlagsdataMedType(grunnlagsdata, GrunnlagsdataType.V1),
                egenAnsatt,
                personIdent
        )
    }

    fun hentGjeldeneNavn(identer: List<String>): Map<String, String> {
        if (identer.isEmpty()) return emptyMap()
        logger.info("Henter navn til {} personer", identer.size)
        return personService.hentPdlPersonKort(identer).map { it.key to it.value.navn.gjeldende().visningsnavn() }.toMap()
    }

    fun hentNavKontor(ident: String): NavKontorEnhet {
        return familieIntegrasjonerClient.hentNavKontor(ident)
    }
}
