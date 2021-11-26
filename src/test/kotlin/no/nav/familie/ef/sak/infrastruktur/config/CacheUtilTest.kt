package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

interface CacheService {

    fun getValue(i: List<Int>): Map<Int, Int>
}

internal class CacheUtilTest {

    private val cacheManager = ConcurrentMapCacheManager()
    private val mock = mockk<CacheService>()

    @BeforeEach
    internal fun setUp() {
        every { mock.getValue(any()) } answers { firstArg<List<Int>>().associateWith { it } }
    }

    @Test
    internal fun `skal hente verdier som ikke er cachet fra før`() {
        val cachetVerdier = cacheManager.getCachedOrLoad("noe", listOf(1, 2)) { mock.getValue(it) }
        val cachetVerdier2 = cacheManager.getCachedOrLoad("noe", listOf(1, 4)) { mock.getValue(it) }
        val cachetVerdier3 = cacheManager.getCachedOrLoad("noe", listOf(1, 2, 4)) { mock.getValue(it) }

        assertThat(cachetVerdier).isEqualTo(listOf(1, 2).associateWith { it })
        assertThat(cachetVerdier2).isEqualTo(listOf(1, 4).associateWith { it }) // legger på 4 som ikke er cachet
        assertThat(cachetVerdier3).isEqualTo(listOf(1, 2, 4).associateWith { it })

        verify(exactly = 1) {
            mock.getValue(listOf(1, 2))
            mock.getValue(listOf(4))
        }
    }
}