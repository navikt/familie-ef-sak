package no.nav.familie.ef.sak.repository

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

internal class RepositoryUtilTest {

    private data class TestDomene(val s: String)

    @Repository
    private interface TestRepository : CrudRepository<TestDomene, String>

    @Test
    internal fun `findOrThrow - kaster exception`() {
        val testRepository = mockk<TestRepository>()
        every { testRepository.findById(any()) } returns Optional.empty()
        assertThat(catchThrowable { testRepository.findByIdOrThrow("123") })
                .hasMessage("Finner ikke TestDomene med id=123")
                .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    internal fun `findOrThrow`() {
        val testRepository = mockk<TestRepository>()
        every { testRepository.findById(any()) } returns Optional.of(TestDomene(""))
        assertThat(testRepository.findByIdOrThrow("123")).isNotNull
    }
}