package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.felles.util.PagineringUtil.paginer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PagineringUtilTest {

    @Test
    internal fun `tom liste`() {
        assertThat(paginer(emptyList<Int>(), 1, 1)).isEmpty()
        assertThat(paginer(emptyList<Int>(), 2, 1)).isEmpty()
        assertThat(paginer(emptyList<Int>(), 1, 3)).isEmpty()
    }

    @Test
    internal fun `liste med jevnt antall objekt`() {
        assertThat(paginer(listOf(1, 2), 1, 1)).containsExactly(1)
        assertThat(paginer(listOf(1, 2), 2, 1)).containsExactly(2)
    }

    @Test
    internal fun `liste med ujevnt antall objekt`() {
        assertThat(paginer(listOf(1, 2, 3), 2, 2)).containsExactly(3)
        assertThat(paginer(listOf(1, 2, 3), 3, 2)).isEmpty()
    }

    @Test
    internal fun `skal kaste feil når man sender inn side under 1`() {
        assertThrows<IllegalArgumentException> { paginer(emptyList<Int>(), 0, 1) }
        assertThrows<IllegalArgumentException> { paginer(emptyList<Int>(), -10, 1) }
    }

    @Test
    internal fun `skal kaste feil når man sender inn antallPerSide under 1`() {
        assertThrows<IllegalArgumentException> { paginer(emptyList<Int>(), 1, 0) }
        assertThrows<IllegalArgumentException> { paginer(emptyList<Int>(), 1, -10) }
    }
}