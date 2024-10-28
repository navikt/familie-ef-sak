package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegel
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AktivitetspliktigAlderPåBarnRegelTest {
    val barnId = UUID.randomUUID()
    val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    val behandlingBarnMedDnr = behandlingBarn(behandlingId = UUID.randomUUID(), personIdent = "06431960727", søknadBarnId = UUID.randomUUID(), fødselTermindato = LocalDate.now().minusYears(5))
    val behandlingBarn2 = behandlingBarn(behandlingId = UUID.randomUUID(), personIdent = "03041983106", søknadBarnId = UUID.randomUUID())
    val vilkårGrunnlagDto = mockk<VilkårGrunnlagDto>()
    val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
    val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
    val barnMedSamvær = BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)

    @BeforeEach
    fun setup() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarnMedDnr, behandlingBarn2)
        every { hovedregelMetadataMock.behandling } returns behandling()
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns vilkårGrunnlagDto
        every { vilkårGrunnlagDto.barnMedSamvær } returns listOf(barnMedSamvær)
    }

    @Test
    fun `skal bruke fødselTermindato hvis fødselsdato ikke finnes i registerdata`() {
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
    fun `skal bruke fødselsdato av registerdata hvis datoen finnes der`() {
        every { barnMedSamværRegistergrunnlagDto.fødselsdato } returns LocalDate.now().minusYears(50)
        every { vilkårGrunnlagDto.barnMedSamvær } returns listOf(BarnMedSamværDto(barnId, barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto))
        val listDelvilkårsvurdering =
            AlderPåBarnRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                barnId,
            )
        Assertions.assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Vilkår ikke tatt stilling til og har ikke fullført fjerdetrinn - skal automatisk oppfylle vilkår`() {
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
