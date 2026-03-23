package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException

internal class InsertUpdateRepositoryImplTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Test
    internal fun `skal kaste exception hvis man bruker save eller saveAll`() {
        assertThat(
            catchThrowable {
                @Suppress("DEPRECATION")
                fagsakRepository.save(fagsakDomain())
            },
        ).isInstanceOf(UnsupportedOperationException::class.java)

        assertThat(
            catchThrowable {
                @Suppress("DEPRECATION")
                fagsakRepository.saveAll(listOf(fagsakDomain(), fagsakDomain()))
            },
        ).isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    internal fun `skal lagre entitet`() {
        val person = testoppsettService.opprettPerson(FagsakPerson(identer = emptySet()))
        fagsakRepository.insert(fagsakDomain(personId = person.id))
        assertThat(fagsakRepository.count()).isEqualTo(1)
    }

    @Test
    internal fun `skal lagre entiteter`() {
        val person1 = testoppsettService.opprettPerson(FagsakPerson(identer = emptySet()))
        val person2 = testoppsettService.opprettPerson(FagsakPerson(identer = emptySet()))
        fagsakRepository.insertAll(listOf(fagsakDomain(personId = person1.id), fagsakDomain(personId = person2.id)))
        assertThat(fagsakRepository.count()).isEqualTo(2)
    }

    @Test
    internal fun `skal oppdatere entitet`() {
        val person = testoppsettService.opprettPerson(FagsakPerson(identer = emptySet()))
        val fagsak = fagsakRepository.insert(fagsakDomain(stønadstype = StønadType.BARNETILSYN, personId = person.id))
        fagsakRepository.update(fagsak.copy(stønadstype = StønadType.OVERGANGSSTØNAD))

        assertThat(fagsakRepository.count()).isEqualTo(1)
        fagsakRepository.findAll().forEach {
            assertThat(it.stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)
        }
    }

    @Test
    internal fun `skal oppdatere entiteter`() {
        val person1 = testoppsettService.opprettPerson(FagsakPerson(identer = emptySet()))
        val person2 = testoppsettService.opprettPerson(FagsakPerson(identer = emptySet()))
        val fagsaker =
            fagsakRepository.insertAll(
                listOf(
                    fagsakDomain(stønadstype = StønadType.BARNETILSYN, personId = person1.id),
                    fagsakDomain(stønadstype = StønadType.SKOLEPENGER, personId = person2.id),
                ),
            )
        fagsakRepository.updateAll(fagsaker.map { it.copy(stønadstype = StønadType.OVERGANGSSTØNAD) })

        assertThat(fagsakRepository.count()).isEqualTo(2)
        fagsakRepository.findAll().forEach {
            assertThat(it.stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)
        }
    }

    /**
     * Dersom denne testen slutter å fungere og endretTid oppdateres for alle søkerIdenter ved endring av fagsak/søkerIdenter
     * må vi endre bruken av aktivIdent til å sjekke på opprettetTid - samt jukse med opprettetTid
     * dersom en gammel personIdent gjenbrukes
     */
    @Test
    internal fun `skal ikke oppdatere endretTid på barnEntiteter i collection`() {
        val personIdent = "12345"
        val nyPersonIdent = "1234"
        val annenIdent = "9"
        val person = testoppsettService.opprettPerson(FagsakPerson(identer = setOf(PersonIdent(personIdent))))
        Thread.sleep(200)
        val oppdatertPerson =
            fagsakPersonRepository.update(
                person.copy(
                    identer =
                        person.identer
                            .map { it.copy(ident = nyPersonIdent) }
                            .toSet() +
                            PersonIdent(annenIdent),
                ),
            )
        val oppdatertSøkerIdent = oppdatertPerson.identer.first { it.ident == nyPersonIdent }
        val originalSøkerIdent = person.identer.first { it.ident == personIdent }

        assertThat(originalSøkerIdent.sporbar.endret.endretTid).isEqualTo(oppdatertSøkerIdent.sporbar.endret.endretTid)
        assertThat(originalSøkerIdent.sporbar.opprettetTid).isEqualTo(oppdatertSøkerIdent.sporbar.opprettetTid)
    }

    @Test
    internal fun `insert skal være transactional`() {
        fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        try {
            fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        } catch (e: Exception) {
        }
        assertThat(fagsakPersonRepository.findAll()).hasSize(1)
    }
}
