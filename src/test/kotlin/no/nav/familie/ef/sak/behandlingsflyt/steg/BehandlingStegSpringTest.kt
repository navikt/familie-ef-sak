package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingStegSpringTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var behandlingSteg: List<BehandlingSteg<*>>

    @Test
    internal fun `skal ikke finnes fler enn ett BehandlingSteg per StegType`() {
        assertThat(behandlingSteg).isNotEmpty
        behandlingSteg.groupBy { it.stegType() }.entries
            .forEach {
                assertThat(it.value)
                    .withFailMessage("${it.key} har flere BehandlingSteg: ${it.value}")
                    .hasSize(1)
            }
    }
}
