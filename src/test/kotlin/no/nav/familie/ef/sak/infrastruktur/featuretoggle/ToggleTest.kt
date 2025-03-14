package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ToggleTest {
    @Test
    internal fun `toggles må inneholde unike toggleId`() {
        val togglesMedDuplikat = FeatureToggle.values().groupBy { it.toggleId }.filter { it.value.size > 1 }
        assertThat(togglesMedDuplikat).isEmpty()
    }

    @Test
    internal fun `toggles må ha gyldig toggleId og ikke inneholde åæø`() {
        val regex = """^[a-zA-Z0-9.\-]+$""".toRegex()
        FeatureToggle.values().forEach { toggle ->
            if (toggle.toggleId.isEmpty()) {
                error("Toggle=$toggle mangler toggleId")
            }
            if (!regex.matches(toggle.toggleId)) {
                val ugyldigeTegn =
                    toggle.toggleId
                        .split("")
                        .filter { it.isNotEmpty() }
                        .filterNot { regex.matches(it) }
                        .map { "'$it'" }
                        .toSet()
                error("Toggle=$toggle inneholder ugyldige tegn=$ugyldigeTegn")
            }
        }
    }
}
