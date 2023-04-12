package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.brev.MellomlagerBrevRepository
import no.nav.familie.ef.sak.brev.MellomlagerFritekstbrevRepository
import no.nav.familie.ef.sak.brev.MellomlagerFrittståendeBrevRepository
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.Fritekstbrev
import no.nav.familie.ef.sak.brev.domain.FrittståendeBrevmottakere
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeBrev
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevSanity
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MellomlagringBrevServiceTest {

    private val mellomlagerBrevRepository = mockk<MellomlagerBrevRepository>()
    private val mellomlagerFritekstbrevRepository = mockk<MellomlagerFritekstbrevRepository>()
    private val mellomlagerFrittståendeBrevRepository = mockk<MellomlagerFrittståendeBrevRepository>()
    private val mellomlagringBrevService = no.nav.familie.ef.sak.brev.MellomlagringBrevService(
        mellomlagerBrevRepository,
        mellomlagerFritekstbrevRepository,
        mellomlagerFrittståendeBrevRepository,
    )

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
    }

    @AfterAll
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere mellomlagret brev`() {
        every { mellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns mellomlagretBrev

        assertThat(
            mellomlagringBrevService.hentOgValiderMellomlagretBrev(
                behandlingId,
                sanityVersjon,
            ),
        )
            .isEqualTo(
                MellomlagretBrevSanity(
                    brevmal = mellomlagretBrev.brevmal,
                    brevverdier = mellomlagretBrev.brevverdier,
                ),
            )
    }

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere null når sanityVersjon ikke matcher`() {
        every { mellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns MellomlagretBrev(
            behandlingId,
            brevverdier,
            brevmal,
            "1",
            LocalDate.now(),
        )
        assertThat(mellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId, "2")).isNull()
    }

    @Test
    fun `hentMellomlagretFrittståendeBrev skal returnere mellomlagret frittstående brev`() {
        val fagsakId = UUID.randomUUID()
        val avsnitt = listOf(FrittståendeBrevAvsnitt("del", "innehold", true))
        val brev = MellomlagretFrittståendeBrev(
            id = UUID.randomUUID(),
            fagsakId = fagsakId,
            brev = Fritekstbrev(overskrift = "Hei", avsnitt = avsnitt),
            brevType = FrittståendeBrevKategori.INFORMASJONSBREV,
            saksbehandlerIdent = "Bob",
            mottakere = FrittståendeBrevmottakere(
                listOf(brevmottakerPerson()),
                listOf(brevmottakerOrganisasjon()),
            ),
        )
        every {
            mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSaksbehandlerIdent(
                fagsakId,
                any(),
            )
        } returns brev
        val dto = mellomlagringBrevService.hentMellomlagretFrittståendeBrev(fagsakId)
        assertThat(dto).isNotNull
        assertThat(dto!!.fagsakId).isEqualTo(brev.fagsakId)
        assertThat(dto.avsnitt).isEqualTo(brev.brev.avsnitt)
        assertThat(dto.brevType).isEqualTo(brev.brevType)
        assertThat(dto.mottakere!!.organisasjoner).isEqualTo(brev.mottakere!!.organisasjoner)
        assertThat(dto.mottakere!!.personer).isEqualTo(brev.mottakere!!.personer)
    }

    private fun brevmottakerOrganisasjon() = BrevmottakerOrganisasjon("456", "Power", MottakerRolle.FULLMAKT)
    private fun brevmottakerPerson() = BrevmottakerPerson("123", "Arne", MottakerRolle.VERGE)

    private val behandlingId = UUID.randomUUID()
    private val brevmal = "testMal"
    private val sanityVersjon = "1"
    private val brevverdier = "{}"
    private val mellomlagretBrev = MellomlagretBrev(behandlingId, brevverdier, brevmal, sanityVersjon, LocalDate.now())
}
