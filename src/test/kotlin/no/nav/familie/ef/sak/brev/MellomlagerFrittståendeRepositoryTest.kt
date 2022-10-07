package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.Fritekstbrev
import no.nav.familie.ef.sak.brev.domain.FrittståendeBrevmottakere
import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeBrev
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit

internal class MellomlagerFrittståendeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val mellomlagretBrev = MellomlagretFrittståendeBrev(
            fagsakId = fagsak.id,
            brev = Fritekstbrev(
                overskrift = "Et testbrev",
                avsnitt = listOf(
                    FrittståendeBrevAvsnitt(
                        deloverskrift = "En deloverskift",
                        innhold = "Noe innhold"
                    )
                )
            ),
            brevType = FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT,
            saksbehandlerIdent = "12345678910",
            mottakere = FrittståendeBrevmottakere(
                personer = listOf(brevmottakerPerson()),
                organisasjoner = listOf(brevmottakerOrganisasjon())
            )
        )

        mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev)

        val mellomlagretBrevFraDb = mellomlagerFrittståendeBrevRepository.findById(mellomlagretBrev.id)
        Assertions.assertThat(mellomlagretBrevFraDb)
            .get().usingRecursiveComparison().ignoringFields("tidspunktOpprettet").isEqualTo(mellomlagretBrev)
        Assertions.assertThat(mellomlagretBrevFraDb.get().tidspunktOpprettet)
            .isCloseTo(mellomlagretBrev.tidspunktOpprettet, within(1, ChronoUnit.SECONDS))
    }

    @Test
    internal fun `skal finne igjen mellomlagret brev fra fagsakId og saksbehandlers ident`() {
        val saksbehandlerIdent = "12345678910"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val mellomlagretBrev = MellomlagretFrittståendeBrev(
            fagsakId = fagsak.id,
            brev = Fritekstbrev(
                overskrift = "Et testbrev",
                avsnitt = listOf(
                    FrittståendeBrevAvsnitt(
                        deloverskrift = "En deloverskift",
                        innhold = "Noe innhold"
                    )
                )
            ),
            brevType = FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT,
            saksbehandlerIdent = saksbehandlerIdent,
            mottakere = null
        )

        mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev)

        val mellomlagretBrevFraDb =
            mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSaksbehandlerIdent(fagsak.id, saksbehandlerIdent)
        Assertions.assertThat(mellomlagretBrevFraDb)
            .usingRecursiveComparison()
            .ignoringFields("tidspunktOpprettet")
            .isEqualTo(mellomlagretBrev)
    }

    private fun brevmottakerOrganisasjon() = BrevmottakerOrganisasjon("456", "Power", MottakerRolle.FULLMAKT)
    private fun brevmottakerPerson() = BrevmottakerPerson("123", "Arne", MottakerRolle.VERGE)
}
