package no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class NyttBarnSammePartnerRegelTest {

    val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    val behandlingBarn = behandlingBarn(behandlingId = UUID.randomUUID())
    val behandlingBarn2 = behandlingBarn(behandlingId = UUID.randomUUID())
    val behandlingBarnMedTermindato = behandlingBarn(behandlingId = UUID.randomUUID(), fødselTermindato = LocalDate.now().plusWeeks(5))

    @BeforeEach
    fun setup() {
        every { hovedregelMetadataMock.erSøknadSomBehandlingÅrsak } returns true
        every { hovedregelMetadataMock.harBrukerEllerAnnenForelderTidligereVedtak } returns false
    }
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
    fun `gitt bruker med ett registrert terminbarn, når initereDelvilkårvurdering, så skal vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)
        every { hovedregelMetadataMock.erSøknadSomBehandlingÅrsak } returns true

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
    fun `gitt bruker med ett barn med termindato og tidligere vedtak, når initiereDelvilkårsvurdering, så skal ikke vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarnMedTermindato)
        every { hovedregelMetadataMock.harBrukerEllerAnnenForelderTidligereVedtak } returns true
        every { hovedregelMetadataMock.finnesBarnUtenRegistrertForelder } returns false

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

    @Test
    fun `gitt bruker med to barn, hvorav ett uten annen forelder, når initereDelvilkårvurdering, så skal ikke vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn, behandlingBarn2)

        every { hovedregelMetadataMock.finnesBarnUtenRegistrertForelder } returns true
        every { hovedregelMetadataMock.finnesFlereBarnMedSammeAnnenForelder } returns false

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

    @Test
    fun `gitt bruker med to barn, som ikke er terminbarn, hvor ingen av barna er registrert med samme annen forelder, når initiereDelvilkårvurdering, så skal vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn, behandlingBarn2)
        every { hovedregelMetadataMock.finnesBarnUtenRegistrertForelder } returns false
        every { hovedregelMetadataMock.finnesFlereBarnMedSammeAnnenForelder } returns false

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
    fun `gitt bruker med tre barn, hvorav to barn har samme annen forelder og har tidligere vedtak, når initiereDelvilkårvurdering, så skal ikke vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn, behandlingBarn2, behandlingBarn(behandlingId = UUID.randomUUID()))
        every { hovedregelMetadataMock.finnesBarnUtenRegistrertForelder } returns false
        every { hovedregelMetadataMock.finnesFlereBarnMedSammeAnnenForelder } returns true
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

    @Test
    fun `gitt bruker med to barn og uten tidligere vedtaksperioder, når initereDelvilkårvurdering, så skal vilkår automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn, behandlingBarnMedTermindato)

        every { hovedregelMetadataMock.finnesBarnUtenRegistrertForelder } returns false
        every { hovedregelMetadataMock.finnesFlereBarnMedSammeAnnenForelder } returns false

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

}
