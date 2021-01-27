package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.brev.BrevClient
import org.springframework.stereotype.Service

@Service
class BrevService(private val brevClient: BrevClient) {

    fun lagBrev(): ByteArray {
        return brevClient.genererBrev("bokmål", "dummy", "hei")
    }
}