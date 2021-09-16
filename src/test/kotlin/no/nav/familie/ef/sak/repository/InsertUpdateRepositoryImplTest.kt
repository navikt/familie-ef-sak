package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.fagsak.Stønadstype
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
            fagsakRepository.save(fagsak())
        }).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(catchThrowable {
            fagsakRepository.saveAll(listOf(fagsak(), fagsak()))
        }).isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    internal fun `skal lagre entitet`() {
        fagsakRepository.insert(fagsak())
        assertThat(fagsakRepository.count()).isEqualTo(1)
    }

    @Test
    internal fun `skal lagre entiteter`() {
        fagsakRepository.insertAll(listOf(fagsak(), fagsak()))
        assertThat(fagsakRepository.count()).isEqualTo(2)
    }

    @Test
    internal fun `skal oppdatere entitet`() {
        val fagsak = fagsakRepository.insert(fagsak(stønadstype = Stønadstype.BARNETILSYN))
        fagsakRepository.update(fagsak.copy(stønadstype = Stønadstype.OVERGANGSSTØNAD))

        assertThat(fagsakRepository.count()).isEqualTo(1)
        fagsakRepository.findAll().forEach {
            assertThat(it.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
        }
    }

    @Test
    internal fun `skal oppdatere entiteter`() {
        val fagsaker = fagsakRepository.insertAll(listOf(fagsak(stønadstype = Stønadstype.BARNETILSYN),
                                                         fagsak(stønadstype = Stønadstype.SKOLEPENGER)))
        fagsakRepository.updateAll(fagsaker.map { it.copy(stønadstype = Stønadstype.OVERGANGSSTØNAD) })

        assertThat(fagsakRepository.count()).isEqualTo(2)
        fagsakRepository.findAll().forEach {
            assertThat(it.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
        }
    }

    @Test
    internal fun `skal kaste exception hvis man oppdaterer entiteter som ikke finnes`() {
        assertThat(catchThrowable {
            fagsakRepository.update(fagsak())
        }).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(catchThrowable {
            fagsakRepository.updateAll(listOf(fagsak(), fagsak()))
        }).isInstanceOf(DbActionExecutionException::class.java)
    }
}