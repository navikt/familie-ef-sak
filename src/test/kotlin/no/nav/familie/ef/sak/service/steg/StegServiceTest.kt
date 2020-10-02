package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.CustomRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("integrasjonstest")
internal class StegServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var stegService: StegService

    @Autowired private lateinit var customRepository: CustomRepository

    @Test
    internal fun `skal håndtere en ny søknad`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak))

        stegService.håndterSøknad(behandling, "")
    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er ferdigstilt`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak, steg = StegType.BEHANDLING_FERDIGSTILT))

        assertThrows<IllegalStateException> {
            stegService.håndterSøknad(behandling, "")
        }
    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er sendt til beslutter`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak, steg = StegType.BESLUTTE_VEDTAK))

        assertThrows<IllegalStateException> {
            stegService.håndterSøknad(behandling, "")
        }
    }
}