package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.repository.samværsuke
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.BARNEHAGE_SKOLE
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.ETTERMIDDAG
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.KVELD_NATT
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.MORGEN
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
            """.trimMargin(),
        )
        assertThat(avsnitt2.innhold).isEqualTo(
            """Mandag(4/8) - kveld/natt
                |Tirsdag(4/8) - kveld/natt
                |Onsdag(4/8) - kveld/natt
                |Torsdag(4/8) - kveld/natt
                |Fredag(4/8) - kveld/natt
                |Lørdag(4/8) - kveld/natt
                |Søndag(4/8) - kveld/natt
            """.trimMargin(),
        )
        assertThat(avsnitt3.innhold).isEqualTo(
            """Mandag(5/8) - kveld/natt og morgen
                |Tirsdag(5/8) - kveld/natt og morgen
                |Onsdag(5/8) - kveld/natt og morgen
                |Torsdag(5/8) - kveld/natt og morgen
                |Fredag(5/8) - kveld/natt og morgen
                |Lørdag(5/8) - kveld/natt og morgen
                |Søndag(5/8) - kveld/natt og morgen
            """.trimMargin(),
        )
        assertThat(avsnitt4.innhold).isEqualTo(
            """Mandag(7/8) - kveld/natt, morgen og barnehage/skole
                |Tirsdag(7/8) - kveld/natt, morgen og barnehage/skole
                |Onsdag(7/8) - kveld/natt, morgen og barnehage/skole
                |Torsdag(7/8) - kveld/natt, morgen og barnehage/skole
                |Fredag(7/8) - kveld/natt, morgen og barnehage/skole
                |Lørdag(7/8) - kveld/natt, morgen og barnehage/skole
                |Søndag(7/8) - kveld/natt, morgen og barnehage/skole
            """.trimMargin(),
        )
        assertThat(avsnitt5.innhold).isEqualTo(
            """Mandag(1) - hele dagen
                |Tirsdag(1) - hele dagen
                |Onsdag(1) - hele dagen
                |Torsdag(1) - hele dagen
                |Fredag(1) - hele dagen
                |Lørdag(1) - hele dagen
                |Søndag(1) - hele dagen
            """.trimMargin(),
        )
    }
}
