package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.repository.samværsuke
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.KVELD_NATT
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.MORGEN
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.ETTERMIDDAG
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.BARNEHAGE_SKOLE
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

internal class SamværsavtaleHelperTest {

    @Test
    internal fun `skal mappe fra samværsuke til avsnitt for fritekstbrev`() {
        val samværsuke1 = samværsuke(listOf())
        val samværsuke2 = samværsuke(listOf(KVELD_NATT))
        val samværsuke3 = samværsuke(listOf(KVELD_NATT, MORGEN))
        val samværsuke4 = samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE))
        val samværsuke5 = samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE, ETTERMIDDAG))

        val avsnitt1 = SamværsavtaleHelper.lagAvsnittFritekstbrev(1, samværsuke1)
        val avsnitt2 = SamværsavtaleHelper.lagAvsnittFritekstbrev(2, samværsuke2)
        val avsnitt3 = SamværsavtaleHelper.lagAvsnittFritekstbrev(3, samværsuke3)
        val avsnitt4 = SamværsavtaleHelper.lagAvsnittFritekstbrev(4, samværsuke4)
        val avsnitt5 = SamværsavtaleHelper.lagAvsnittFritekstbrev(5, samværsuke5)

        assertThat(avsnitt1.deloverskrift).isEqualTo("Uke 1")
        assertThat(avsnitt2.deloverskrift).isEqualTo("Uke 2")
        assertThat(avsnitt3.deloverskrift).isEqualTo("Uke 3")
        assertThat(avsnitt4.deloverskrift).isEqualTo("Uke 4")
        assertThat(avsnitt5.deloverskrift).isEqualTo("Uke 5")

        assertThat(avsnitt1.innhold).isEqualTo(
            """Mandag: -
                |Tirsdag: -
                |Onsdag: -
                |Torsdag: -
                |Fredag: -
                |Lørdag: -
                |Søndag: -
        """.trimMargin()
        )
        assertThat(avsnitt2.innhold).isEqualTo(
            """Mandag: kveld/natt
                |Tirsdag: kveld/natt
                |Onsdag: kveld/natt
                |Torsdag: kveld/natt
                |Fredag: kveld/natt
                |Lørdag: kveld/natt
                |Søndag: kveld/natt
        """.trimMargin()
        )
        assertThat(avsnitt3.innhold).isEqualTo(
            """Mandag: kveld/natt, morgen
                |Tirsdag: kveld/natt, morgen
                |Onsdag: kveld/natt, morgen
                |Torsdag: kveld/natt, morgen
                |Fredag: kveld/natt, morgen
                |Lørdag: kveld/natt, morgen
                |Søndag: kveld/natt, morgen
        """.trimMargin()
        )
        assertThat(avsnitt4.innhold).isEqualTo(
            """Mandag: kveld/natt, morgen, barnehage/skole
                |Tirsdag: kveld/natt, morgen, barnehage/skole
                |Onsdag: kveld/natt, morgen, barnehage/skole
                |Torsdag: kveld/natt, morgen, barnehage/skole
                |Fredag: kveld/natt, morgen, barnehage/skole
                |Lørdag: kveld/natt, morgen, barnehage/skole
                |Søndag: kveld/natt, morgen, barnehage/skole
        """.trimMargin()
        )
        assertThat(avsnitt5.innhold).isEqualTo(
            """Mandag: kveld/natt, morgen, barnehage/skole, ettermiddag
                |Tirsdag: kveld/natt, morgen, barnehage/skole, ettermiddag
                |Onsdag: kveld/natt, morgen, barnehage/skole, ettermiddag
                |Torsdag: kveld/natt, morgen, barnehage/skole, ettermiddag
                |Fredag: kveld/natt, morgen, barnehage/skole, ettermiddag
                |Lørdag: kveld/natt, morgen, barnehage/skole, ettermiddag
                |Søndag: kveld/natt, morgen, barnehage/skole, ettermiddag
        """.trimMargin()
        )
    }
}