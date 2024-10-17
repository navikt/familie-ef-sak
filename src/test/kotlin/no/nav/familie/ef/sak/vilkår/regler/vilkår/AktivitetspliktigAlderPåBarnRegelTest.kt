package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegel
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AktivitetspliktigAlderPåBarnRegelTest {
    val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    val behandlingBarnMedDnr = behandlingBarn(behandlingId = UUID.randomUUID(), personIdent = "06431960727", søknadBarnId = UUID.randomUUID())
    val behandlingBarn2 = behandlingBarn(behandlingId = UUID.randomUUID(), personIdent = "03041983106", søknadBarnId = UUID.randomUUID())

    @BeforeEach
    fun setup() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarnMedDnr, behandlingBarn2)
        every { hovedregelMetadataMock.behandling } returns behandling()
    }

    @Test
    fun `Vilkår ikke tatt stilling til og har fullført fjerdetrinn - skal automatisk oppfylle vilkår`() {
        val listDelvilkårsvurdering =
            AlderPåBarnRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                behandlingBarnMedDnr.id,
            )

        Assertions.assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Vilkår tatt stilling til i resultat, skal ikke vurdere automatisk`() {
        val listDelvilkårsvurdering =
            AlderPåBarnRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.OPPFYLT,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }
}
