package no.nav.familie.ef.sak.opplysninger.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class PersonServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val cache = ConcurrentMapCacheManager()

    private val personService = PersonService(pdlClient, cache)

    @BeforeEach
    internal fun setUp() {
        every { pdlClient.hentPersonKortBolk(any()) } answers { firstArg<List<String>>().associateWith { mockk() } }
    }

    @Test
    internal fun `hentPdlPersonKort - skal cachea svar`() {
        personService.hentPdlPersonKort(listOf("1", "2"))
        personService.hentPdlPersonKort(listOf("1"))
        verify(exactly = 1) {
            pdlClient.hentPersonKortBolk(any())
        }
    }
}