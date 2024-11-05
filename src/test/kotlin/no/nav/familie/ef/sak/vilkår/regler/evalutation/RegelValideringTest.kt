package no.nav.familie.ef.sak.vilkår.regler.evalutation

import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegelValideringTest {
    @Test
    fun `sender inn en tom liste med svar - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThat(
            Assertions.catchThrowable {
                valider(regel, *emptyArray<VurderingDto>())
            },
        ).hasMessage("List is empty.")
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `sender in svar med feil rootId - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThat(
            Assertions.catchThrowable {
                valider(
                    regel,
                    VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE),
                )
            },
        ).hasMessageStartingWith("Delvilkårsvurderinger savner svar på hovedregler")
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in 2 svar men mangler svarId på første - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThat(
            Assertions.catchThrowable {
                valider(
                    regel,
                    VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE),
                    VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE),
                )
            },
        ).hasMessage(
            "Mangler svar på ett spørsmål som ikke er siste besvarte spørsmålet vilkårType=ALENEOMSORG " +
                "regelId=BOR_OG_OPPHOLDER_SEG_I_NORGE",
        ).isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in fler svar enn det finnes mulighet for - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThat(
            Assertions.catchThrowable {
                valider(
                    regel,
                    VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI),
                    VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.NEI),
                    VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE),
                )
            },
        ).hasMessageStartingWith("Finnes ikke noen flere regler, men finnes flere svar")
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `regelId for det andre spørsmålet er feil - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThat(
            Assertions.catchThrowable {
                valider(
                    regel,
                    VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI),
                    VurderingDto(RegelId.NÆRE_BOFORHOLD, SvarId.NEI),
                )
            },
        ).hasMessage("Finner ikke regelId=NÆRE_BOFORHOLD for vilkårType=ALENEOMSORG")
            .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `har begrunnelse på ett spørsmål som ikke skal ha begrunnelse - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThat(
            Assertions.catchThrowable {
                valider(
                    regel,
                    VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA, "b"),
                )
            },
        ).hasMessage(
            "Begrunnelse for vilkårType=ALENEOMSORG regelId=BOR_OG_OPPHOLDER_SEG_I_NORGE " +
                "svarId=JA skal ikke ha begrunnelse",
        ).isInstanceOf(Feil::class.java)
    }

    @Test
    fun `har en tom begrunnelse på ett spørsmål som ikke skal ha begrunnelse - skal kaste exception`() {
        val regel = VilkårsregelEnHovedregel()

        assertThat(
            Assertions.catchThrowable {
                valider(
                    regel,
                    VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA, "      "),
                )
            },
        ).hasMessage(
            "Begrunnelse for vilkårType=ALENEOMSORG regelId=BOR_OG_OPPHOLDER_SEG_I_NORGE " +
                "svarId=JA skal ikke ha begrunnelse",
        ).isInstanceOf(Feil::class.java)
    }

    private fun valider(
        regel: Vilkårsregel,
        vararg vurderinger: VurderingDto,
    ) {
        valider(regel, delvilkårsvurderingDto(*vurderinger))
    }

    private fun valider(
        regel: Vilkårsregel,
        vararg delvilkårsvurderingDto: DelvilkårsvurderingDto,
    ) {
        RegelValidering.validerVurdering(
            vilkårsregel = regel,
            oppdatering = delvilkårsvurderingDto.toList(),
            tidligereDelvilkårsvurderinger = regel.initiereDelvilkårsvurdering(mockk()),
        )
    }
}
