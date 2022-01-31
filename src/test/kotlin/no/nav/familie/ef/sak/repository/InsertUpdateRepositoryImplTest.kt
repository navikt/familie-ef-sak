package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException

internal class InsertUpdateRepositoryImplTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Test
    internal fun `skal kaste exception hvis man bruker save eller saveAll`() {
        assertThat(catchThrowable {
            fagsakRepository.save(fagsakDao())
        }).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(catchThrowable {
            fagsakRepository.saveAll(listOf(fagsakDao(), fagsakDao()))
        }).isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    internal fun `skal lagre entitet`() {
        fagsakRepository.insert(fagsakDao())
        assertThat(fagsakRepository.count()).isEqualTo(1)
    }

    @Test
    internal fun `skal lagre entiteter`() {
        fagsakRepository.insertAll(listOf(fagsakDao(), fagsakDao()))
        assertThat(fagsakRepository.count()).isEqualTo(2)
    }

    @Test
    internal fun `skal oppdatere entitet`() {
        val fagsak = fagsakRepository.insert(fagsakDao(stønadstype = Stønadstype.BARNETILSYN))
        fagsakRepository.update(fagsak.copy(stønadstype = Stønadstype.OVERGANGSSTØNAD))

        assertThat(fagsakRepository.count()).isEqualTo(1)
        fagsakRepository.findAll().forEach {
            assertThat(it.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
        }
    }

    @Test
    internal fun `skal oppdatere entiteter`() {
        val fagsaker = fagsakRepository.insertAll(listOf(fagsakDao(stønadstype = Stønadstype.BARNETILSYN),
                                                         fagsakDao(stønadstype = Stønadstype.SKOLEPENGER)))
        fagsakRepository.updateAll(fagsaker.map { it.copy(stønadstype = Stønadstype.OVERGANGSSTØNAD) })

        assertThat(fagsakRepository.count()).isEqualTo(2)
        fagsakRepository.findAll().forEach {
            assertThat(it.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
        }
    }


    /**
     * Dersom denne testen slutter å fungere og endretTid oppdateres for alle søkerIdenter ved endring av fagsak/søkerIdenter
     * må vi endre bruken av aktivIdent til å sjekke på opprettetTid - samt jukse med opprettetTid
     * dersom en gammel personIdent gjenbrukes
     */
    @Test
    internal fun `skal oppdatere endretTid på rot-entitet, men ikke barne-entiteter `() {
        val personIdent = "12345"
        val nyPersonIdent = "1234"
        val fagsak = fagsakRepository.insert(fagsakDao(stønadstype = Stønadstype.BARNETILSYN,
                                                    identer = setOf(FagsakPerson(personIdent))))
        Thread.sleep(200)
        val oppdatertFagsak = fagsakRepository.update(
                fagsak.copy(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                            søkerIdenter = fagsak.søkerIdenter.map { it.copy(ident = nyPersonIdent) }
                                                   .toSet() + FagsakPerson("99999"))
        )
        val oppdatertSøkerIdent = oppdatertFagsak.søkerIdenter.first { it.ident == nyPersonIdent }
        val originalSøkerIdent = fagsak.søkerIdenter.first { it.ident == personIdent }

        assertThat(fagsak.sporbar.endret.endretTid).isBefore(oppdatertFagsak.sporbar.endret.endretTid)
        assertThat(oppdatertFagsak.sporbar.opprettetTid).isBefore(oppdatertFagsak.sporbar.endret.endretTid)
        assertThat(oppdatertFagsak.sporbar.opprettetTid).isEqualTo(fagsak.sporbar.opprettetTid)

        assertThat(originalSøkerIdent.sporbar.endret.endretTid).isEqualTo(oppdatertSøkerIdent.sporbar.endret.endretTid)
        assertThat(originalSøkerIdent.sporbar.opprettetTid).isEqualTo(oppdatertSøkerIdent.sporbar.opprettetTid)
    }

    @Test
    internal fun `skal kaste exception hvis man oppdaterer entiteter som ikke finnes`() {
        assertThat(catchThrowable {
            fagsakRepository.update(fagsakDao())
        }).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(catchThrowable {
            fagsakRepository.updateAll(listOf(fagsakDao(), fagsakDao()))
        }).isInstanceOf(DbActionExecutionException::class.java)
    }
}