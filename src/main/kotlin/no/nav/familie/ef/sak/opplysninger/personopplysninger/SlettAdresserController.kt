package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/fjern-adresser"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
@Validated
class SlettAdresserController(
    private val pdlClient: PdlClient,
    private val fagsakService: FagsakService,
    private val grunnlagsdataRepository: GrunnlagsdataRepository
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    @PostMapping
    fun slettData(@RequestBody identer: Set<String>) {
        identer.forEach { aktørId ->
            val identer = pdlClient.hentPersonidenter(aktørId, true)
            val pdlData = pdlClient.hentSøker(identer.gjeldende().ident)

            if (pdlData.adressebeskyttelse.gjeldende()?.erStrengtFortrolig() == true) {
                val fagsaker = fagsakService.finnFagsaker(identer.identer())
                if (fagsaker.isNotEmpty()) {
                    fagsaker.forEach { fagsak ->
                        val behandlinger = fagsakService.fagsakTilDto(fagsak).behandlinger

                        behandlinger.forEach { behandling -> fjernDataForBehandling(behandling.id) }
                    }
                }
            } else {
                secureLogger.info("aktør=$aktørId er ikke strengt fortrolig")
            }
        }
    }

    private fun fjernDataForBehandling(behandlingId: UUID) {
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrNull(behandlingId)
        if (grunnlagsdata != null) {
            val søker = grunnlagsdata.data.søker
            if (
                søker.bostedsadresse.isNotEmpty() ||
                søker.utflyttingFraNorge.isNotEmpty() ||
                søker.innflyttingTilNorge.isNotEmpty()
            ) {
                secureLogger.info("Sletter data for behandling=$behandlingId")
                val oppdatertData = grunnlagsdata.data.copy(
                    søker = søker.copy(
                        bostedsadresse = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList()
                    )
                )
                val antallOppdatert = grunnlagsdataRepository.oppdaterData(behandlingId, oppdatertData)
                feilHvis(antallOppdatert != 1) {
                    "Antall oppdatert for behandling=$behandlingId er $antallOppdatert"
                }
            }
        }
    }
}