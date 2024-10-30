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
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelVersjon
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AleneomsorgRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test

class AleneomsorgRegelTest {
    private val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    private val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
    private val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()

    @BeforeEach
    fun setup() {
        val behandling = behandling()
        every { hovedregelMetadataMock.behandling } returns behandling
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto()
        every { hovedregelMetadataMock.langAvstandTilSøker } returns emptyList()
    }

    val barnId = UUID.randomUUID()

    @Test
    fun `Vilkårsregel SKRIFTLIG_AVTALE_OM_DELT_BOSTED sin versjon skal være HISTORISK`() {
        val skriftligAvtaleRegel = AleneomsorgRegel().regel(RegelId.SKRIFTLIG_AVTALE_OM_DELT_BOSTED)
        Assertions.assertThat(skriftligAvtaleRegel.versjon).isEqualTo(RegelVersjon.HISTORISK)
    }

    @Test
    fun `Skal automatisk vurdere vilkår om aleneomsorg når det er digital søknad, donorbarn og har samme adresse`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.SØKNAD)
        every { barnMedSamværSøknadsgrunnlagDto.ikkeOppgittAnnenForelderBegrunnelse } returns "donor"
        every { barnMedSamværRegistergrunnlagDto.harSammeAdresse } returns true
        every { barnMedSamværSøknadsgrunnlagDto.harSammeAdresse } returns false

        val registerBarn = BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                barnMedSamvær = listOf(registerBarn),
            )

        val listDelvilkårsvurdering =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                barnId = barnId,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal automatisk vurdere vilkår om aleneomsorg når det er terminbarn og donorbarn og svart skal bo sammen i søknaden`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.SØKNAD)
        every { barnMedSamværSøknadsgrunnlagDto.ikkeOppgittAnnenForelderBegrunnelse } returns "donor"
        every { barnMedSamværRegistergrunnlagDto.harSammeAdresse } returns null
        every { barnMedSamværSøknadsgrunnlagDto.erTerminbarn() } returns true
        every { barnMedSamværSøknadsgrunnlagDto.harSammeAdresse } returns true

        val registerBarn = BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                barnMedSamvær = listOf(registerBarn),
            )

        val listDelvilkårsvurdering =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                barnId = barnId,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår om aleneomsorg når det er terminbarn men ikke svart at ja på skal bo sammen i søknaden`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.SØKNAD)
        every { barnMedSamværSøknadsgrunnlagDto.ikkeOppgittAnnenForelderBegrunnelse } returns "annet"
        every { barnMedSamværRegistergrunnlagDto.harSammeAdresse } returns null
        every { barnMedSamværSøknadsgrunnlagDto.erTerminbarn() } returns true
        every { barnMedSamværSøknadsgrunnlagDto.harSammeAdresse } returns false

        val registerBarn = BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                barnMedSamvær = listOf(registerBarn),
            )

        val listDelvilkårsvurdering =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                barnId = barnId,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår om aleneomsorg når ikkeOppgittAnnenForelderBegrunnelse er annet`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.SØKNAD)
        every { barnMedSamværSøknadsgrunnlagDto.ikkeOppgittAnnenForelderBegrunnelse } returns "annet"
        every { barnMedSamværSøknadsgrunnlagDto.erTerminbarn() } returns true
        every { barnMedSamværRegistergrunnlagDto.harSammeAdresse } returns true
        every { barnMedSamværSøknadsgrunnlagDto.harSammeAdresse } returns false

        val registerBarn = BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                barnMedSamvær = listOf(registerBarn),
            )

        val listDelvilkårsvurdering =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                barnId = barnId,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår om aleneomsorg når det ikke er digital søknad`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.PAPIRSØKNAD)
        every { barnMedSamværSøknadsgrunnlagDto.ikkeOppgittAnnenForelderBegrunnelse } returns "donor"

        val registerBarn = BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                barnMedSamvær = listOf(registerBarn),
            )

        val listDelvilkårsvurdering =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                barnId = barnId,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår om aleneomsorg når barnet og søker ikke har samme adresse`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.SØKNAD)
        every { barnMedSamværSøknadsgrunnlagDto.ikkeOppgittAnnenForelderBegrunnelse } returns "donor"
        every { barnMedSamværRegistergrunnlagDto.harSammeAdresse } returns false
        every { barnMedSamværSøknadsgrunnlagDto.erTerminbarn() } returns true
        every { barnMedSamværSøknadsgrunnlagDto.harSammeAdresse } returns false

        val registerBarn = BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                barnMedSamvær = listOf(registerBarn),
            )

        val listDelvilkårsvurdering =
            AleneomsorgRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                barnId = barnId,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}
