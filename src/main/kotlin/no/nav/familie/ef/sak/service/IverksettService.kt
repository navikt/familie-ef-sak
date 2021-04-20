package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.IverksettClient
import org.springframework.stereotype.Service

@Service
class IverksettService(val iverksettClient: IverksettClient) {

    fun test(): String {
        return iverksettClient.hentTestrespons()
    }
}
