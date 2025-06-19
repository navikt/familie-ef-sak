package no.nav.familie.ef.sak.no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.brev.FrittståendeBrevmottakereRepository
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.BrevmottakereFrittståendeBrev
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.brev.domain.OrganisasjonerWrapper
import no.nav.familie.ef.sak.brev.domain.PersonerWrapper
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit

internal class FrittståendeBrevmottakereRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var frittståendeBrevmottakereRepository: FrittståendeBrevmottakereRepository

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    internal fun `skal lagre brevmottaker for fagsak`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val brevmottakere =
            BrevmottakereFrittståendeBrev(
                fagsakId = fagsak.id,
                personer =
                    PersonerWrapper(
                        listOf(
                            BrevmottakerPerson(
                                personIdent = "12345678910",
                                navn = "Verge",
                                mottakerRolle = MottakerRolle.VERGE,
                            ),
                        ),
                    ),
                organisasjoner =
                    OrganisasjonerWrapper(
                        listOf(
                            BrevmottakerOrganisasjon(
                                organisasjonsnummer = "12345678",
                                navnHosOrganisasjon = "Advokat",
                                MottakerRolle.FULLMEKTIG,
                            ),
                        ),
                    ),
            )

        frittståendeBrevmottakereRepository.insert(brevmottakere)

        val brevmottakereFraDb =
            frittståendeBrevmottakereRepository.findByFagsakIdAndSaksbehandlerIdent(
                fagsak.id,
                SikkerhetContext.hentSaksbehandler(),
            )

        assertThat(brevmottakereFraDb).usingRecursiveComparison().ignoringFields("tidspunktOpprettet").isEqualTo(brevmottakere)
        assertThat(brevmottakereFraDb!!.tidspunktOpprettet).isCloseTo(brevmottakere.tidspunktOpprettet, within(1, ChronoUnit.SECONDS))
    }
}
