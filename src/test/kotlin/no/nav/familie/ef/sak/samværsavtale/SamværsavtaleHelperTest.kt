package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.repository.samværsavtale
import no.nav.familie.ef.sak.repository.samværsuke
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.BARNEHAGE_SKOLE
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.ETTERMIDDAG
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.KVELD_NATT
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.MORGEN
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import kotlin.test.Test

internal class SamværsavtaleHelperTest {
    @Test
    internal fun `skal mappe fra samværsuke til avsnitt for fritekstbrev`() {
        val samværsuke1 = samværsuke(listOf())
        val samværsuke2 = samværsuke(listOf(KVELD_NATT))
        val samværsuke3 = samværsuke(listOf(KVELD_NATT, MORGEN))
        val samværsuke4 = samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE))
        val samværsuke5 = samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE, ETTERMIDDAG))

        val avsnitt1 = SamværsavtaleHelper.lagAvsnitt(1, samværsuke1)
        val avsnitt2 = SamværsavtaleHelper.lagAvsnitt(2, samværsuke2)
        val avsnitt3 = SamværsavtaleHelper.lagAvsnitt(3, samværsuke3)
        val avsnitt4 = SamværsavtaleHelper.lagAvsnitt(4, samværsuke4)
        val avsnitt5 = SamværsavtaleHelper.lagAvsnitt(5, samværsuke5)

        assertThat(avsnitt1.deloverskrift).isEqualTo("Uke 1")
        assertThat(avsnitt2.deloverskrift).isEqualTo("Uke 2")
        assertThat(avsnitt3.deloverskrift).isEqualTo("Uke 3")
        assertThat(avsnitt4.deloverskrift).isEqualTo("Uke 4")
        assertThat(avsnitt5.deloverskrift).isEqualTo("Uke 5")

        assertThat(avsnitt1.innhold).isEqualTo(
            """Mandag (0)
                |Tirsdag (0)
                |Onsdag (0)
                |Torsdag (0)
                |Fredag (0)
                |Lørdag (0)
                |Søndag (0)
            """.trimMargin(),
        )
        assertThat(avsnitt2.innhold).isEqualTo(
            """Mandag (4/8) - kveld/natt
                |Tirsdag (4/8) - kveld/natt
                |Onsdag (4/8) - kveld/natt
                |Torsdag (4/8) - kveld/natt
                |Fredag (4/8) - kveld/natt
                |Lørdag (4/8) - kveld/natt
                |Søndag (4/8) - kveld/natt
            """.trimMargin(),
        )
        assertThat(avsnitt3.innhold).isEqualTo(
            """Mandag (5/8) - kveld/natt og morgen
                |Tirsdag (5/8) - kveld/natt og morgen
                |Onsdag (5/8) - kveld/natt og morgen
                |Torsdag (5/8) - kveld/natt og morgen
                |Fredag (5/8) - kveld/natt og morgen
                |Lørdag (5/8) - kveld/natt og morgen
                |Søndag (5/8) - kveld/natt og morgen
            """.trimMargin(),
        )
        assertThat(avsnitt4.innhold).isEqualTo(
            """Mandag (7/8) - kveld/natt, morgen og barnehage/skole
                |Tirsdag (7/8) - kveld/natt, morgen og barnehage/skole
                |Onsdag (7/8) - kveld/natt, morgen og barnehage/skole
                |Torsdag (7/8) - kveld/natt, morgen og barnehage/skole
                |Fredag (7/8) - kveld/natt, morgen og barnehage/skole
                |Lørdag (7/8) - kveld/natt, morgen og barnehage/skole
                |Søndag (7/8) - kveld/natt, morgen og barnehage/skole
            """.trimMargin(),
        )
        assertThat(avsnitt5.innhold).isEqualTo(
            """Mandag (1) - hele dagen
                |Tirsdag (1) - hele dagen
                |Onsdag (1) - hele dagen
                |Torsdag (1) - hele dagen
                |Fredag (1) - hele dagen
                |Lørdag (1) - hele dagen
                |Søndag (1) - hele dagen
            """.trimMargin(),
        )
    }

    @Test
    internal fun `skal mappe fra samværsavtale til tekstlig oppsummering for blankett`() {
        val behandlingBarnId = UUID.randomUUID()
        val samværsuker =
            listOf(
                samværsuke(),
                samværsuke(listOf(ETTERMIDDAG)),
                samværsuke(listOf(ETTERMIDDAG, KVELD_NATT)),
                samværsuke(listOf(ETTERMIDDAG, KVELD_NATT, BARNEHAGE_SKOLE)),
                samværsuke(listOf(ETTERMIDDAG, KVELD_NATT, BARNEHAGE_SKOLE, MORGEN)),
            )
        val samværsavtale = samværsavtale(behandlingBarnid = behandlingBarnId, uker = samværsuker)

        val beregnetSamvær = SamværsavtaleHelper.lagBeregnetSamvær(samværsavtale)

        assertThat(beregnetSamvær.behandlingBarnId).isEqualTo(behandlingBarnId)
        assertThat(beregnetSamvær.uker.size).isEqualTo(5)

        assertThat(beregnetSamvær.uker.get(0).deloverskrift).isEqualTo("Uke 1")
        assertThat(beregnetSamvær.uker.get(0).innhold).isEqualTo(
            """Mandag (0)
                |Tirsdag (0)
                |Onsdag (0)
                |Torsdag (0)
                |Fredag (0)
                |Lørdag (0)
                |Søndag (0)
            """.trimMargin(),
        )

        assertThat(beregnetSamvær.uker.get(1).deloverskrift).isEqualTo("Uke 2")
        assertThat(beregnetSamvær.uker.get(1).innhold).isEqualTo(
            """Mandag (1/8) - ettermiddag
                |Tirsdag (1/8) - ettermiddag
                |Onsdag (1/8) - ettermiddag
                |Torsdag (1/8) - ettermiddag
                |Fredag (1/8) - ettermiddag
                |Lørdag (1/8) - ettermiddag
                |Søndag (1/8) - ettermiddag
            """.trimMargin(),
        )

        assertThat(beregnetSamvær.uker.get(2).deloverskrift).isEqualTo("Uke 3")
        assertThat(beregnetSamvær.uker.get(2).innhold).isEqualTo(
            """Mandag (5/8) - ettermiddag og kveld/natt
                |Tirsdag (5/8) - ettermiddag og kveld/natt
                |Onsdag (5/8) - ettermiddag og kveld/natt
                |Torsdag (5/8) - ettermiddag og kveld/natt
                |Fredag (5/8) - ettermiddag og kveld/natt
                |Lørdag (5/8) - ettermiddag og kveld/natt
                |Søndag (5/8) - ettermiddag og kveld/natt
            """.trimMargin(),
        )

        assertThat(beregnetSamvær.uker.get(3).deloverskrift).isEqualTo("Uke 4")
        assertThat(beregnetSamvær.uker.get(3).innhold).isEqualTo(
            """Mandag (7/8) - ettermiddag, kveld/natt og barnehage/skole
                |Tirsdag (7/8) - ettermiddag, kveld/natt og barnehage/skole
                |Onsdag (7/8) - ettermiddag, kveld/natt og barnehage/skole
                |Torsdag (7/8) - ettermiddag, kveld/natt og barnehage/skole
                |Fredag (7/8) - ettermiddag, kveld/natt og barnehage/skole
                |Lørdag (7/8) - ettermiddag, kveld/natt og barnehage/skole
                |Søndag (7/8) - ettermiddag, kveld/natt og barnehage/skole
            """.trimMargin(),
        )

        assertThat(beregnetSamvær.uker.get(4).deloverskrift).isEqualTo("Uke 5")
        assertThat(beregnetSamvær.uker.get(4).innhold).isEqualTo(
            """Mandag (1) - hele dagen
                |Tirsdag (1) - hele dagen
                |Onsdag (1) - hele dagen
                |Torsdag (1) - hele dagen
                |Fredag (1) - hele dagen
                |Lørdag (1) - hele dagen
                |Søndag (1) - hele dagen
            """.trimMargin(),
        )

        assertThat(beregnetSamvær.oppsummering).isEqualTo("Samvær: 18 dager og 3/8 deler av totalt 5 uker = 52.5%")
    }
}
