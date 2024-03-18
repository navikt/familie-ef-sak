package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.OppholdINorgeRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OppholdINorgeRegelTest {
    private val hovedregelMetadataMock = mockk<HovedregelMetadata>()

    val barnMedSamværList = barnMedSamværListe(2)

    @BeforeEach
    fun setup() {
        val behandling = behandling()
        every { hovedregelMetadataMock.behandling } returns behandling
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap = medlemskapDto(),
                barnMedSamvær = barnMedSamværList,
            )
    }

    @Test
    fun `Skal automatisk oppfylle vilkår om opphold i Norge når det er en digital søknad, søker er norsk statsborger, søker oppholder seg i Norge, har personstatus bosatt, og alle barn har personstatus bosatt`() {
        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår når søker ikke er norsk statsborger`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                medlemskap = medlemskapDto(land = "sverige"),
                barnMedSamvær = barnMedSamværList,
            )

        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal automatisk oppfylle vilkår for søker uten eksisterende barn, men terminbarn`() {
        val terminbarnRegisterGrunnlag = mockk<BarnMedSamværRegistergrunnlagDto>()
        every { terminbarnRegisterGrunnlag.folkeregisterpersonstatus } returns null
        val terminbarnSøknadGrunnlag = terminbarnSøknadsgrunnlag()
        val terminbarn = BarnMedSamværDto(UUID.randomUUID(), terminbarnSøknadGrunnlag, terminbarnRegisterGrunnlag)

        val barnMedSamværListe = listOf(terminbarn)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap = medlemskapDto(),
                barnMedSamvær = barnMedSamværListe,
            )

        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal automatisk oppfylle vilkår for søker med eksisterende barn som er ok, og et terminbarn`() {
        val terminbarnRegisterGrunnlag = mockk<BarnMedSamværRegistergrunnlagDto>()
        every { terminbarnRegisterGrunnlag.folkeregisterpersonstatus } returns null
        val terminbarnSøknadGrunnlag = terminbarnSøknadsgrunnlag()
        val terminbarn = BarnMedSamværDto(UUID.randomUUID(), terminbarnSøknadGrunnlag, terminbarnRegisterGrunnlag)

        val barnMedSamværListe = listOf(terminbarn) + barnMedSamværList

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap = medlemskapDto(),
                barnMedSamvær = barnMedSamværListe,
            )

        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår når ett av barna ikke har personstatus som bosatt`() {
        val barnUtenFolkeregisterPersonStatus =
            barnMedSamværList.last().copy(
                registergrunnlag = barnMedSamværList.last().registergrunnlag.copy(folkeregisterpersonstatus = null),
            )
        val barnMedSamværListe = listOf(barnMedSamværList.first(), barnUtenFolkeregisterPersonStatus)

        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap = medlemskapDto(),
                barnMedSamvær = barnMedSamværListe,
            )

        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår når søknaden ikke er digital`() {
        val behandling = behandling(årsak = BehandlingÅrsak.PAPIRSØKNAD)

        every {
            hovedregelMetadataMock.behandling
        } returns behandling

        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår når søker har svart nei på spørsmål om opphold i Norge i søknaden`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                medlemskap = medlemskapDto(oppholderDuDegINorge = false),
                barnMedSamvær = barnMedSamværList,
            )

        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår når søker har personstatus utflyttet`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                medlemskap = medlemskapDto(folkeregisterpersonstatus = Folkeregisterpersonstatus.UTFLYTTET),
                barnMedSamvær = barnMedSamværList,
            )

        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}
