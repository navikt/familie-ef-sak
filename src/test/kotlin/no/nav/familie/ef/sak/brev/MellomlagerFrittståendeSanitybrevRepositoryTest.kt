package no.nav.familie.ef.sak.no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.brev.MellomlagerFrittståendeSanitybrevRepository
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeSanitybrev
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit

internal class MellomlagerFrittståendeSanitybrevRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var mellomlagerFrittståendeSanitybrevRepository: MellomlagerFrittståendeSanitybrevRepository

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val mellomlagretBrev =
            MellomlagretFrittståendeSanitybrev(
                fagsakId = fagsak.id,
                brevverdier = "{}",
                brevmal = "",
                saksbehandlerIdent = "saksbehandler123",
            )

        mellomlagerFrittståendeSanitybrevRepository.insert(mellomlagretBrev)

        val mellomlagretBrevFraDb = mellomlagerFrittståendeSanitybrevRepository.findById(mellomlagretBrev.id)
        Assertions
            .assertThat(mellomlagretBrevFraDb)
            .get()
            .usingRecursiveComparison()
            .ignoringFields("opprettetTid")
            .isEqualTo(mellomlagretBrev)
        Assertions
            .assertThat(mellomlagretBrevFraDb.get().opprettetTid)
            .isCloseTo(mellomlagretBrev.opprettetTid, within(1, ChronoUnit.SECONDS))
    }

    @Test
    internal fun `skal finne igjen mellomlagret brev fra fagsakId og saksbehandlers ident`() {
        val saksbehandlerIdent = "12345678910"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val mellomlagretBrev =
            MellomlagretFrittståendeSanitybrev(
                fagsakId = fagsak.id,
                brevverdier = "{}",
                brevmal = "",
                saksbehandlerIdent = saksbehandlerIdent,
            )

        mellomlagerFrittståendeSanitybrevRepository.insert(mellomlagretBrev)
        val mellomlagretBrevFraDb =
            mellomlagerFrittståendeSanitybrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsak.id, saksbehandlerIdent)
        Assertions
            .assertThat(mellomlagretBrevFraDb)
            .usingRecursiveComparison()
            .ignoringFields("opprettetTid")
            .isEqualTo(mellomlagretBrev)
    }
}
