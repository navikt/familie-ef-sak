package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.MorEllerFarRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MorEllerFarRegelTest {

    private val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    private val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
    private val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
    @BeforeEach
    fun setup() {
        val behandling = behandling()
        every { hovedregelMetadataMock.behandling } returns behandling
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto()
        every { barnMedSamværRegistergrunnlagDto.fødselsnummer } returns "01010199999"

    }

    @Test
    fun `Automatisk vurder mor eller far vilkår med kun registerbarn for digital søknad`() {

        val registerBarn = BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto(
            barnMedSamvær = listOf(registerBarn),
        )

        val listDelvilkårsvurdering = MorEllerFarRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        )

        Assertions.assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Terminbarn i søknad skal ikke automatisk vurderes`() {
        val registerBarn = BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

        val terminbarnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
        every { terminbarnMedSamværRegistergrunnlagDto.fødselsnummer } returns null
        val terminbarn =
            BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, terminbarnMedSamværRegistergrunnlagDto)

        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto(
            barnMedSamvær = listOf(terminbarn, registerBarn),
        )

        val listDelvilkårsvurdering = AlderPåBarnRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        )

        Assertions.assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Papirsøknad skal ikke automatisk vurderes`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.PAPIRSØKNAD)

        val listDelvilkårsvurdering = MorEllerFarRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        )

        Assertions.assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}
