package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.repository.BrevRepository
import no.nav.familie.ef.sak.repository.domain.Brev
import org.springframework.stereotype.Service

@Service
class BrevService(private val brevClient: BrevClient, private val brevRepository: BrevRepository) {

    fun lagBrev(): ByteArray {
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


        /*
        * Logikk for brevgenering her
        */
        val brev = Brev(pdf = brevClient.genererBrev("brukesIkke", "brukesIkke", body));
        brevRepository.insert(brev);

        return brev.pdf;
    }
}