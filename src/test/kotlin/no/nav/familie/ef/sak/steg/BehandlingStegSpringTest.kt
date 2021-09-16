package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.steg.BehandlingSteg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingStegSpringTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingSteg: List<BehandlingSteg<*>>

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
