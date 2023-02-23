package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.AnnenForelderDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.TidligereInnvilgetVedtakDto
import no.nav.familie.ef.sak.vilkår.dto.TidligereVedtaksperioderDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.vilkår.NyttBarnSammePartnerRegel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class NyttBarnSammePartnerRegelTest {

    val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    val behandlingBarn = behandlingBarn(behandlingId = UUID.randomUUID())

    @BeforeEach
    fun setup() {
        every { hovedregelMetadataMock.behandling } returns behandling()
    }

    @Test
    fun `Gitt at bruker eller annen foreldre ikke har tidligere vedtak, og alle barn har registrert annen forelder, når initiereDelvilkår, så skal vilkåret automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto()

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
    fun `Gitt at bruker eller annen foreldre har tidligere vedtak, og alle barn har registrert annen forelder, når initiereDelvilkår, så skal vilkåret ikke automatisk oppfylles`() {
        val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
        val annenForelderDto = mockk<AnnenForelderDto>()
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto(
            barnMedSamvær = listOf(BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, mockk())),
            tidligereVedtaksperioder = TidligereVedtaksperioderDto(TidligereInnvilgetVedtakDto(true, false, false), TidligereInnvilgetVedtakDto(false, false, false), false)
        )
        every { annenForelderDto.tidligereVedtaksperioder } returns TidligereVedtaksperioderDto(null, null, false)
        every { barnMedSamværSøknadsgrunnlagDto.forelder } returns annenForelderDto
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
    fun `Gitt at bruker eller annen foreldre ikke har tidligere vedtak, og det finnes barn uten registrert annen forelder, når initiereDelvilkår, så skal vilkåret ikke automatisk oppfylles`() {
        val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
        val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
        val annenForelderDto = mockk<AnnenForelderDto>()
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto(
            barnMedSamvær = listOf(BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto))
        )
        every { annenForelderDto.tidligereVedtaksperioder } returns TidligereVedtaksperioderDto(null, null, false)
        every { barnMedSamværSøknadsgrunnlagDto.forelder } returns annenForelderDto
        every { barnMedSamværRegistergrunnlagDto.forelder } returns null
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
