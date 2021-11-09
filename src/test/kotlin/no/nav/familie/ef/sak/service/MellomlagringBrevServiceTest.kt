package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.MellomlagerBrevRepository
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevSanity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID


internal class MellomlagringBrevServiceTest {

    private val mellomlagerBrevRepository = mockk<MellomlagerBrevRepository>()
    private val mellomlagringBrevService = no.nav.familie.ef.sak.brev.MellomlagringBrevService(mellomlagerBrevRepository, mockk(), mockk())


    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere mellomlagret brev`() {

        every { mellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns mellomlagretBrev

        assertThat(mellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId,
                                                                          sanityVersjon))
                .isEqualTo(MellomlagretBrevSanity(brevmal = mellomlagretBrev.brevmal,
                                                  brevverdier = mellomlagretBrev.brevverdier))
    }

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere null når sanityVersjon ikke matcher`() {
        every { mellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns MellomlagretBrev(behandlingId,
                                                                                                  brevverdier,
                                                                                                  brevmal,
                                                                                                  "1",
                                                                                                  LocalDate.now())
        assertThat(mellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId, "2")).isNull()
    }

    private val behandlingId = UUID.randomUUID()
    private val brevmal = "testMal"
    private val sanityVersjon = "1"
    private val brevverdier = "{}"
    private val mellomlagretBrev = MellomlagretBrev(behandlingId, brevverdier, brevmal, sanityVersjon, LocalDate.now())
}