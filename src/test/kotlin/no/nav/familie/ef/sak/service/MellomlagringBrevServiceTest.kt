package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.repository.MellomlagerBrevRepository
import no.nav.familie.ef.sak.repository.domain.MellomlagretBrev
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID


internal class MellomlagringBrevServiceTest {

    val MellomlagerBrevRepository = mockk<MellomlagerBrevRepository>()
    val MellomlagringBrevService = MellomlagringBrevService(MellomlagerBrevRepository)


    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere mellomlagret brev`() {

        every { MellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns mellomlagretBrev

        assertThat(MellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId,
                                                                          brevmal,
                                                                          sanityVersjon)).usingRecursiveComparison()
                .isEqualTo(mellomlagretBrev)

    }

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere null når sanityVersjon ikke matcher`() {
        every { MellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns MellomlagretBrev(behandlingId,
                                                                                                  brevverdier,
                                                                                                  brevmal,
                                                                                                  "1")

        assertThat(MellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId, brevmal, "2")).isNull()
    }

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere null når brevmal ikke matcher`() {
        every { MellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns MellomlagretBrev(behandlingId,
                                                                                                  brevverdier,
                                                                                                  "enAnnenBrevmal",
                                                                                                  sanityVersjon)

        assertThat(MellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId, brevmal, sanityVersjon)).isNull()
    }

    private val behandlingId = UUID.randomUUID()
    val brevmal = "testMal"
    val sanityVersjon = "1"
    val brevverdier = "{}"
    private val mellomlagretBrev = MellomlagretBrev(behandlingId, brevverdier, brevmal, sanityVersjon)

}