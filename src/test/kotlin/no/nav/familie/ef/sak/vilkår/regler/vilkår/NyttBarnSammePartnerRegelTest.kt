package no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class NyttBarnSammePartnerRegelTest {

    val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    val behandlingBarn = behandlingBarn(behandlingId = UUID.randomUUID())
    val behandlingBarnMedTermindato = behandlingBarn(behandlingId = UUID.randomUUID(), fødselTermindato = LocalDate.now().plusWeeks(5))

    @Test
    fun `gitt bruker med ett barn, uten registrert terminbarn, når initereDelvilkårvurdering, så skal vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)

        val listDelvilkårsvurdering = NyttBarnSammePartnerRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            null
        )

        assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
        assertThat(listDelvilkårsvurdering.first().vurderinger.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().svar).isEqualTo(SvarId.NEI)
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().begrunnelse).startsWith("Automatisk vurdert (${LocalDate.now().norskFormat()}):")
    }

    @Test
    fun `gitt bruker med flere enn ett barn og uten tidligere vedtaksperioder, når initereDelvilkårvurdering, så skal vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn, behandlingBarnMedTermindato)
        every { hovedregelMetadataMock.harBrukerEllerAnnenForelderTidligereVedtak } returns false

        val listDelvilkårsvurdering = NyttBarnSammePartnerRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            null
        )

        assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
        assertThat(listDelvilkårsvurdering.first().vurderinger.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().svar).isEqualTo(SvarId.NEI)
        assertThat(listDelvilkårsvurdering.first().vurderinger.first().begrunnelse).startsWith("Automatisk vurdert (${LocalDate.now().norskFormat()}):")
    }

    @Test
    fun `initiereDelvilkårsvurdering - kan ikke automatisk vurdere bruker med ett barn med termindato og tidligere vedtak`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarnMedTermindato)
        every { hovedregelMetadataMock.harBrukerEllerAnnenForelderTidligereVedtak } returns true

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
