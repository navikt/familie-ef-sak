package no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NyttBarnSammePartnerRegelTest {

    val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    val behandlingBarn = mockk<BehandlingBarn>()

    @Test
    fun `initiereDelvilkårsvurdering - automatisk vurdert`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)
        every { hovedregelMetadataMock.terminbarnISøknad } returns false

        val listDelvilkårsvurdering = NyttBarnSammePartnerRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            null
        )

        assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
        assertThat(listDelvilkårsvurdering.first().vurderinger.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().svar).isEqualTo(SvarId.NEI)
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().begrunnelse).startsWith("Automatisk vurdert:")
    }

    @Test
    fun `initiereDelvilkårsvurdering - kan ikke automatisk vurdere`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)
        every { hovedregelMetadataMock.terminbarnISøknad } returns true

        val listDelvilkårsvurdering = NyttBarnSammePartnerRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            null
        )

        assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(listDelvilkårsvurdering.first().vurderinger.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().svar).isNull()
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().begrunnelse).isNull()
    }

}
