package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.brev.BrevClient
import org.springframework.stereotype.Service

@Service
class BrevService(private val brevClient: BrevClient) {

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

        return brevClient.genererBrev("brukesIkke", "brukesIkke", body)
    }
}