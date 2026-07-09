package no.nav.familie.ef.sak.no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.brev.MellomlagerFrittståendeSanitybrevRepository
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeSanitybrev
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

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

    @Test
    internal fun `upsert skal oppdatere eksisterende mellomlagret brev uten å feile på duplicate key`() {
        val saksbehandlerIdent = "12345678910"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val opprinneligDato = LocalDateTime.now().minusDays(1)

        mellomlagerFrittståendeSanitybrevRepository.upsert(
            UUID.randomUUID(),
            fagsak.id,
            "{}",
            "mal",
            saksbehandlerIdent,
            opprinneligDato,
        )
        mellomlagerFrittståendeSanitybrevRepository.upsert(
            UUID.randomUUID(),
            fagsak.id,
            "{\"felt\":1}",
            "mal2",
            saksbehandlerIdent,
            LocalDateTime.now(),
        )

        val lagret = mellomlagerFrittståendeSanitybrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsak.id, saksbehandlerIdent)
        assertThat(lagret).isNotNull
        assertThat(lagret!!.brevverdier).isEqualTo("{\"felt\":1}")
        assertThat(lagret.brevmal).isEqualTo("mal2")
    }
}
