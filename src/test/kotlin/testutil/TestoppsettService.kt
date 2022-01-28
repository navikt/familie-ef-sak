package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class TestoppsettService(
        private val fagsakRepository: FagsakRepository,
) {

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        return fagsakRepository.insert(fagsak)
    }
    
}