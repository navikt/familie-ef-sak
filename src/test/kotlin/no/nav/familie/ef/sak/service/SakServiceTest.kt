package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad
import no.nav.familie.ef.sak.repository.VedleggRepository
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.util.*

@ActiveProfiles("local", "mock-oauth")
@TestPropertySource(properties = ["FAMILIE_INTEGRASJONER_URL=http://localhost:28085"])
internal class SakServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var sakService: SakService
    @Autowired lateinit var vedleggRepository: VedleggRepository

    @Test
    internal fun `lagrer sak med vedlegg, vedlegg lagres med id som den kommer inn med`() {
        val vedlegg = Testsøknad.vedlegg
        val sak = SakRequest(søknad = SøknadMedVedlegg(Testsøknad.søknad, vedlegg),
                             saksnummer = "saksnummer",
                             journalpostId = "journalpostId")
        val sakId = sakService.mottaSakOvergangsstønad(sak, mapOf(vedlegg.first().id to "filinnehold".toByteArray()))

        val hentSak = sakService.hentOvergangsstønad(sakId)
        assertThat(hentSak.søknad.personalia).isNotNull
        val hentetVedlegg = vedleggRepository.findByIdOrNull(UUID.fromString(vedlegg.first().id))!!
        assertThat(sakId).isEqualTo(hentetVedlegg.sakId)
        assertThat(String(hentetVedlegg.data)).isEqualTo("filinnehold")
    }
}
