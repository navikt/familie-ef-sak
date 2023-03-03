package no.nav.familie.ef.sak.vilkår.regler.vilkår

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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class NyttBarnSammePartnerRegelTest {

    val hovedregelMetadataMock = mockk<HovedregelMetadata>()
    val behandlingBarn = behandlingBarn(behandlingId = UUID.randomUUID())
    val behandlingBarn2 = behandlingBarn(behandlingId = UUID.randomUUID())

    @BeforeEach
    fun setup() {
        every { hovedregelMetadataMock.skalAutomatiskVurdereNyttBarnSammePartner } returns true
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn, behandlingBarn2)
        every { hovedregelMetadataMock.behandling } returns behandling()
    }

    @Test
    fun `Gitt at bruker har kun ett barn og uavhengig av tidligere vedtak, når initiereDelvilkår, så skal vilkåret automatisk oppfylles`() {
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn)
        val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
        val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
        val annenForelderDto = mockk<AnnenForelderDto>()
        every { annenForelderDto.tidligereVedtaksperioder } returns TidligereVedtaksperioderDto(null, TidligereInnvilgetVedtakDto(false, true, false), false)
        every { barnMedSamværSøknadsgrunnlagDto.forelder } returns annenForelderDto
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto(
            barnMedSamvær = listOf(BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)),
            tidligereVedtaksperioder = TidligereVedtaksperioderDto(TidligereInnvilgetVedtakDto(true, false, false), TidligereInnvilgetVedtakDto(false, false, false), false)
        )

        val listDelvilkårsvurdering = NyttBarnSammePartnerRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            null
        )

        assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
        val delvilkårsvurdering = listDelvilkårsvurdering.first().vurderinger.first()
        assertThat(delvilkårsvurdering.svar).isEqualTo(SvarId.NEI)
        assertThat(delvilkårsvurdering.begrunnelse).isEqualTo("Automatisk vurdert (${LocalDate.now().norskFormat()}): Bruker har kun ett barn.")
    }

    @Test
    fun `Gitt at bruker eller annen foreldre ikke har tidligere vedtak, og alle barn har registrert annen forelder, når initiereDelvilkår, så skal vilkåret automatisk oppfylles`() {
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto()

        val listDelvilkårsvurdering = NyttBarnSammePartnerRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            null
        )

        assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
        assertThat(listDelvilkårsvurdering.first().vurderinger.size).isEqualTo(1)
        val delvilkårsvurdering = listDelvilkårsvurdering.first().vurderinger.first()
        assertThat(delvilkårsvurdering.svar).isEqualTo(SvarId.NEI)
        assertThat(delvilkårsvurdering.begrunnelse).isEqualTo("Automatisk vurdert (${LocalDate.now().norskFormat()}): Verken bruker eller annen forelder får eller har fått stønad for felles barn.")
    }

    @Test
    fun `Gitt at bruker med flere barn eller annen foreldre har tidligere vedtak, og alle barn har registrert annen forelder, når initiereDelvilkår, så skal vilkåret ikke automatisk oppfylles`() {
        val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
        val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
        val annenForelderDto = mockk<AnnenForelderDto>()

        every { barnMedSamværRegistergrunnlagDto.fødselsnummer } returns "01010199999"
        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto(
            barnMedSamvær = listOf(BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)),
            tidligereVedtaksperioder = TidligereVedtaksperioderDto(
                TidligereInnvilgetVedtakDto(true, false, false),
                TidligereInnvilgetVedtakDto(false, false, false),
                false
            )
        )
        every { annenForelderDto.tidligereVedtaksperioder } returns TidligereVedtaksperioderDto(null, null, false)
        every { barnMedSamværSøknadsgrunnlagDto.forelder } returns annenForelderDto
        every { barnMedSamværRegistergrunnlagDto.forelder } returns annenForelderDto
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
    fun `Gitt at bruker med flere barn eller annen foreldre ikke har tidligere vedtak, og det finnes barn uten registrert annen forelder, når initiereDelvilkår, så skal vilkåret ikke automatisk oppfylles`() {
        val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()
        val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
        val annenForelderDto = mockk<AnnenForelderDto>()

        every { barnMedSamværRegistergrunnlagDto.fødselsnummer } returns "01010199999"
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

    @Test
    fun `Gitt bruker uten tidligere vedtak med to barn, hvorav ett terminbarn uten annen forelder, når initieredelvilkår, så skal vilkåret automatisk oppfylles`() {
        // Poenget med testen er at den ikke skal bry seg om at det ikke er registrert annen forelder på terminbarn
        val behandlingBarn = behandlingBarn(behandlingId = UUID.randomUUID())
        val behandlingBarnTerminbarn = behandlingBarn(behandlingId = UUID.randomUUID(), fødselTermindato = LocalDate.now().plusWeeks(8))
        every { hovedregelMetadataMock.barn } returns listOf(behandlingBarn, behandlingBarnTerminbarn)

        val barnMedSamværSøknadsgrunnlagDto = mockk<BarnMedSamværSøknadsgrunnlagDto>()

        val annenForelderDto = mockk<AnnenForelderDto>()
        every { annenForelderDto.fødselsnummer } returns "01010199999"
        every { annenForelderDto.tidligereVedtaksperioder } returns tidligereVedtaksperioderDtoIkkeTidligereInnvilget()

        val barnMedSamværRegistergrunnlagDto = mockk<BarnMedSamværRegistergrunnlagDto>()
        every { barnMedSamværRegistergrunnlagDto.forelder } returns annenForelderDto
        every { barnMedSamværRegistergrunnlagDto.fødselsnummer } returns "01010199999"

        every { hovedregelMetadataMock.vilkårgrunnlagDto } returns VilkårTestUtil.mockVilkårGrunnlagDto(
            barnMedSamvær = listOf(BarnMedSamværDto(UUID.randomUUID(), barnMedSamværSøknadsgrunnlagDto, barnMedSamværRegistergrunnlagDto)),
            tidligereVedtaksperioder = tidligereVedtaksperioderDtoIkkeTidligereInnvilget()
        )
        val annenForelderUtenFødselsnummer = mockk<AnnenForelderDto>()
        every { barnMedSamværSøknadsgrunnlagDto.forelder } returns annenForelderUtenFødselsnummer
        every { annenForelderUtenFødselsnummer.fødselsnummer } returns null
        val listDelvilkårsvurdering = NyttBarnSammePartnerRegel().initiereDelvilkårsvurdering(
            hovedregelMetadataMock,
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            null
        )

        assertThat(listDelvilkårsvurdering.size).isEqualTo(1)
        assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
        assertThat(listDelvilkårsvurdering.first().vurderinger.size).isEqualTo(1)
        val delvilkårsvurdering = listDelvilkårsvurdering.first().vurderinger.first()
        assertThat(delvilkårsvurdering.svar).isEqualTo(SvarId.NEI)
        assertThat(delvilkårsvurdering.begrunnelse).isEqualTo("Automatisk vurdert (${LocalDate.now().norskFormat()}): Verken bruker eller annen forelder får eller har fått stønad for felles barn.")
    }

    private fun tidligereVedtaksperioderDtoIkkeTidligereInnvilget(): TidligereVedtaksperioderDto {
        return TidligereVedtaksperioderDto(
            TidligereInnvilgetVedtakDto(false, false, false),
            TidligereInnvilgetVedtakDto(false, false, false),
            false
        )
    }
}
