package no.nav.familie.ef.sak.regler

import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VilkårsregelTest {

    /**
     * Hvis en
     */
    @Test
    internal fun `sjekker att output fortsatt er det samme på json`() {
        val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()
        alleVilkårsregler.forEach {
            val json = objectWriter.writeValueAsString(it)
            // kommentere ut hvis regler har endret seg for å lagre de nye reglene
            //File("src/test/resources/regler/${it.vilkårType}.json").writeText(json)
            val fileJson = readFile(it)
            assertThat(json).isEqualTo(fileJson)
        }
    }

    private fun readFile(it: Vilkårsregel) =
            this::class.java.classLoader.getResource("regler/${it.vilkårType}.json").readText()
}