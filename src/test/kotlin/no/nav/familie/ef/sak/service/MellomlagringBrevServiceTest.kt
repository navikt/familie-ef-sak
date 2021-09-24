package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.MellomlagerBrevRepository
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID


internal class MellomlagringBrevServiceTest {

    val MellomlagerBrevRepository = mockk<MellomlagerBrevRepository>()
    val MellomlagringBrevService = no.nav.familie.ef.sak.brev.MellomlagringBrevService(MellomlagerBrevRepository)


    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere mellomlagret brev`() {

        every { MellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns mellomlagretBrev

        assertThat(MellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId,
                                                                          sanityVersjon)).usingRecursiveComparison()
                .isEqualTo(mellomlagretBrev)

    }

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere null når sanityVersjon ikke matcher`() {
        every { MellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns MellomlagretBrev(behandlingId,
                                                                                                  brevverdier,
                                                                                                  brevmal,
                                                                                                  "1",
                                                                                                  LocalDate.now())
        assertThat(MellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId, "2")).isNull()
    }

    private val behandlingId = UUID.randomUUID()
    val brevmal = "testMal"
    val sanityVersjon = "1"
    val brevverdier = "{}"
    private val mellomlagretBrev = MellomlagretBrev(behandlingId, brevverdier, brevmal, sanityVersjon, LocalDate.now())
}