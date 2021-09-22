package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.vedtak.MellomlagerBrevRepository
import no.nav.familie.ef.sak.vedtak.domain.MellomlagretBrev
import no.nav.familie.ef.sak.vedtak.MellomlagringBrevService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID


internal class MellomlagringBrevServiceTest {

    val MellomlagerBrevRepository = mockk<MellomlagerBrevRepository>()
    val MellomlagringBrevService = MellomlagringBrevService(MellomlagerBrevRepository)


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

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere null når brevmal ikke matcher`() {
        every { MellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns MellomlagretBrev(behandlingId,
                                                                                                  brevverdier,
                                                                                                  "enAnnenBrevmal",
                                                                                                  sanityVersjon,
                                                                                                  LocalDate.now())

        assertThat(MellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId, sanityVersjon)).isNull()
    }

    private val behandlingId = UUID.randomUUID()
    val brevmal = "testMal"
    val sanityVersjon = "1"
    val brevverdier = "{}"
    private val mellomlagretBrev = MellomlagretBrev(behandlingId, brevverdier, brevmal, sanityVersjon, LocalDate.now())

}