package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.repository.BrevRepository
import no.nav.familie.ef.sak.repository.domain.Brev
import no.nav.familie.ef.sak.repository.domain.EksternBehandlingId
import org.springframework.stereotype.Service
import java.util.*

@Service
class BrevService(private val brevClient: BrevClient,
                  private val brevRepository: BrevRepository,
                  private val behandlingService: BehandlingService,
                  private val fagsakService: FagsakService,
                  private val personService: PersonService) {

    fun lagBrev(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val person = personService.hentSøker(fagsak.hentAktivIdent())
        val navn = person.navn.get(0)

        val request = BrevRequest(navn = "${navn.fornavn} ${navn.etternavn}", ident = fagsak.hentAktivIdent())


        /*
        * Logikk for brevgenering her
        */

        val brev = brevRepository.insert(Brev(behandling = behandlingId,
                                              pdf = brevClient.genererBrev("brukesIkke",
                                                                           "brukesIkke",
                                                                           request)))
        return brev.pdf;
    }

    fun hentBrev(behandlingId: UUID): Brev? {
        return brevRepository.findByBehandlingId(behandlingId)
    }
}

data class BrevRequest(val navn: String, val ident: String) {

    fun lagBody(): String {
        return """
            {
                "flettefelter": {
                    "navn": [
                        "$navn"
            ],
            "fodselsnummer": [
                "$ident"
            ],
            "dato": [
                "01.01.1986"
            ]
        }
            }
        """.trimIndent()


    }
}