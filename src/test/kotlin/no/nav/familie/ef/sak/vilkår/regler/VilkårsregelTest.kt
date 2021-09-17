package no.nav.familie.ef.sak.vilkår.regler

import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

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
            //skrivTilFil(it, json)
            val fileJson = readFile(it)
            assertThat(json).isEqualTo(fileJson)
        }
    }

    private fun skrivTilFil(it: Vilkårsregel, json: String) {
        val file = File("src/test/resources/regler/${it.vilkårType}.json")
        if(!file.exists()) {
            file.createNewFile()
        }
        file.writeText(json)
    }

    @Test
    @Disabled
    internal fun `print alle vilkår`() {
        val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()
        println(objectWriter.writeValueAsString(Vilkårsregler.VILKÅRSREGLER))
    }

    private fun readFile(it: Vilkårsregel) =
            this::class.java.classLoader.getResource("regler/${it.vilkårType}.json").readText()
}