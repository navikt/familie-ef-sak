package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ToggleTest {

    @Test
    internal fun `toggles må inneholde unike toggleId`() {
        val togglesMedDuplikat = Toggle.values().groupBy { it.toggleId }.filter { it.value.size > 1 }
        assertThat(togglesMedDuplikat).isEmpty()
    }
}