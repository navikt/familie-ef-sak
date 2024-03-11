package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.OppholdINorgeRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `Skal automatisk oppfylle vilkår om opphold i Norge når det er en digital søknad, søker oppholder seg i Norge, har personstatus bosatt, og alle barn har samme adresse som søker`() {
        val listDelvilkårsvurdering =
            OppholdINorgeRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår når ikke alle barn har samme adresse som søker`() {
        val barnUtenSammeAdresse =
            barnMedSamværList.last().copy(
                registergrunnlag = barnMedSamværList.last().registergrunnlag.copy(harSammeAdresse = false),
            )
        val barnMedSamværListe = listOf(barnMedSamværList.first(), barnUtenSammeAdresse)

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
