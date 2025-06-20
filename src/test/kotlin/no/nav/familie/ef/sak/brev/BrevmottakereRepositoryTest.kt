package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.Brevmottakere
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.brev.domain.OrganisasjonerWrapper
import no.nav.familie.ef.sak.brev.domain.PersonerWrapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevmottakereRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var brevmottakereRepository: BrevmottakereRepository

    @Test
    internal fun `skal lagre brevmottaker`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val brevmottakere =
            Brevmottakere(
                behandlingId = behandling.id,
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

        brevmottakereRepository.insert(brevmottakere)

        Assertions
            .assertThat(brevmottakereRepository.findById(behandling.id))
            .get()
            .usingRecursiveComparison()
            .isEqualTo(brevmottakere)
    }
}
