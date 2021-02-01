package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.repository.BrevRepository
import no.nav.familie.ef.sak.repository.domain.Brev
import no.nav.familie.ef.sak.repository.domain.EksternBehandlingId
import org.springframework.stereotype.Service
import java.util.*

@Service
class BrevService(private val brevClient: BrevClient, private val brevRepository: BrevRepository) {

    object Constants {

        val body = """
            {
                "flettefelter": {
                    "navn": [
                        "Navn Navnesen"
                    ],
                    "fodselsnummer": [
                        "1123456789"
                    ],
                    "dato": [
                        "01.01.1986"
                    ]
                }
            }
        """.trimIndent()
    }


    fun lagBrev(behandlingId: UUID): ByteArray {

        /*
        * Logikk for brevgenering her
        */
        val brev = brevRepository.insert(Brev(behandling = behandlingId,
                                              pdf = brevClient.genererBrev("brukesIkke",
                                                                           "brukesIkke",
                                                                           Constants.body)))
        return brev.pdf;
    }

    fun hentBrev(behandlingId: UUID): Brev? {
        return brevRepository.findByBehandlingId(behandlingId)
    }


}