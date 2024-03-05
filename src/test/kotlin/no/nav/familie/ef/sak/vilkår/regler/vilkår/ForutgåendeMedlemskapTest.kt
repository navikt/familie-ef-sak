package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.dto.MedlUnntakDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.PersonaliaDto
import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.ForutgåendeMedlemskapRegel
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ForutgåendeMedlemskapTest {
    private val hovedregelMetadataMock = mockk<HovedregelMetadata>()

    @BeforeEach
    fun setup() {
        val behandling = behandling()
        every { hovedregelMetadataMock.behandling } returns behandling
    }

    @Test
    fun `Skal automatisk vurdere forutgående medlemskap når fødeland er Norge, folkeregisterpersonstatus er bosatt og statsborgerskap er Norge`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap = medlemskapDto(),
            )
        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal automatisk vurdere forutgående medlemskap når fødeland er Norge, folkeregisterpersonstatus er bosatt og statsborgerskap er Norge, samt at søker har bodd i Norge de siste fem årene`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap =
                medlemskapDto(
                    innflytting = listOf(InnflyttingDto("fraflyttingsland", LocalDate.now().minusYears(6))),
                ),
            )

        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal automatisk vurdere forutgående medlemskap når fødeland er Norge, folkeregisterpersonstatus er bosatt og statsborgerskap er Norge, samt at søker har bodd i Norge de siste fem årene med utflytting`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap =
                medlemskapDto(
                    innflytting =
                    listOf(
                        InnflyttingDto("fraflyttingsland", LocalDate.now().minusYears(10)),
                        InnflyttingDto("fraflyttingsland", LocalDate.now().minusYears(6)),
                    ),
                    utflytting = listOf(UtflyttingDto("tilflyttingsland", LocalDate.now().minusYears(7))),
                ),
            )

        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår for forutgående medlemskap når folkeregisterpersonstatus er utflyttet`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap =
                medlemskapDto(
                    innflytting =
                    listOf(
                        InnflyttingDto("fraflyttingsland", LocalDate.now().minusYears(10)),
                        InnflyttingDto("fraflyttingsland", LocalDate.now().minusYears(6)),
                    ),
                    utflytting = listOf(UtflyttingDto("tilflyttingsland", LocalDate.now().minusYears(7))),
                    folkeregisterpersonstatus = Folkeregisterpersonstatus.UTFLYTTET,
                ),
            )

        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår forutgående medlemskap når fødeland er noe annet enn Norge`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(fødeland = "noe annet"),
                medlemskap = medlemskapDto(),
            )

        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår forutgående medlemskap når statsborgerskap er noe annet enn Norge`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap = medlemskapDto(land = "noe annet"),
            )

        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal automatisk vurdere forutgående medlemskap når det er søknadsgrunnlag, og søkeren har svart at de har vært bosatt og oppholdt seg i Norge de siste årene`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap =
                medlemskapDto(
                    søknadsgrunnlag =
                    MedlemskapSøknadsgrunnlagDto(
                        bosattNorgeSisteÅrene = true,
                        oppholderDuDegINorge = true,
                        "noe annet",
                        emptyList(),
                    ),
                ),
            )

        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Skal ikke ta stilling til vilkår for forutgående medlemskap når det er søknadsgrunnlag, men søkeren har svart at de ikke har vært bosatt og oppholdt seg i Norge de siste årene`() {
        every {
            hovedregelMetadataMock.vilkårgrunnlagDto
        } returns
            VilkårTestUtil.mockVilkårGrunnlagDto(
                registergrunnlag = personaliaDto(),
                medlemskap =
                medlemskapDto(
                    søknadsgrunnlag =
                    MedlemskapSøknadsgrunnlagDto(
                        bosattNorgeSisteÅrene = false,
                        oppholderDuDegINorge = false,
                        "noe annet",
                        emptyList(),
                    ),
                ),
            )

        val listDelvilkårsvurdering =
            ForutgåendeMedlemskapRegel().initiereDelvilkårsvurdering(
                hovedregelMetadataMock,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                null,
            )

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    private fun medlemskapDto(
        land: String = "norge",
        folkeregisterpersonstatus: Folkeregisterpersonstatus = Folkeregisterpersonstatus.BOSATT,
        innflytting: List<InnflyttingDto> = emptyList(),
        utflytting: List<UtflyttingDto> = emptyList(),
        søknadsgrunnlag: MedlemskapSøknadsgrunnlagDto? = null,
    ) = MedlemskapDto(
        søknadsgrunnlag = søknadsgrunnlag,
        registergrunnlag =
        MedlemskapRegistergrunnlagDto(
            nåværendeStatsborgerskap = listOf(),
            statsborgerskap = listOf(StatsborgerskapDto(land = land, null, null)),
            oppholdstatus = emptyList(),
            bostedsadresse = emptyList(),
            innflytting = innflytting,
            utflytting = utflytting,
            folkeregisterpersonstatus = folkeregisterpersonstatus,
            medlUnntak = MedlUnntakDto(emptyList()),
        ),
    )

    private fun personaliaDto(fødeland: String = "NOR") =
        PersonaliaDto(
            navn = NavnDto(fornavn = "Ola", null, etternavn = "etternavn", visningsnavn = "visningsnavn"),
            personIdent = "111111111",
            bostedsadresse = null,
            fødeland = fødeland,
        )
}
