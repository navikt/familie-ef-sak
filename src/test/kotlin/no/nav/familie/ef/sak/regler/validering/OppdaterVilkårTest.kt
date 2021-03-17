package no.nav.familie.ef.sak.regler.validering

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.VilkårSvarDto
import no.nav.familie.ef.sak.regler.BegrunnelseType
import no.nav.familie.ef.sak.regler.NesteRegel
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.RegelSteg
import no.nav.familie.ef.sak.regler.SluttRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.jaNeiMapping
import no.nav.familie.ef.sak.regler.validering.OppdaterVilkår.utledVilkårResultat
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårSvar
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.util.UUID

class OppdaterVilkårTest {


    @Test
    fun `utledVilkårResultat - er OPPFYLT når alle vilkår er OPPFYLT`() {
        assertThat(utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT)))
                .isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er IKKE_OPPFYLT når det finnes en med IKKE_OPPFYLT`() {
        assertThat(utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                                             RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT)))
                .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er IKKE_TATT_STILLING_TIL når det finnes en med IKKE_TATT_STILLING_TIL`() {
        assertThat(utledVilkårResultat(mapOf(RegelId.OPPHOLD_UNNTAK to Vilkårsresultat.OPPFYLT,
                                             RegelId.MEDLEMSKAP_UNNTAK to Vilkårsresultat.IKKE_OPPFYLT,
                                             RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE to Vilkårsresultat.IKKE_TATT_STILLING_TIL)))
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `sender in en tom liste med svar - skal kaste exception`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        assertThat(catchThrowable { validerOgOppdater(vilkårsvurdering, regel, *emptyArray<VilkårSvarDto>()) })
                .hasMessage("Savner svar for en av delvilkåren for vilkår=ALENEOMSORG")
                .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in svar med feil rootId - skal kaste exception`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        assertThat(catchThrowable {
            validerOgOppdater(vilkårsvurdering, regel,
                              VilkårSvarDto(RegelId.KRAV_SIVILSTAND))
        })
                .hasMessageStartingWith("Delvilkårsvurderinger savner svar på rotregler")
                .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in 2 svar men mangler svarId på første - skal kaste exception`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        assertThat(catchThrowable {
            validerOgOppdater(vilkårsvurdering, regel,
                              VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE),
                              VilkårSvarDto(RegelId.KRAV_SIVILSTAND))
        })
                .hasMessage("Mangler svar på ett spørsmål som ikke er siste besvarte spørsmålet vilkårType=ALENEOMSORG")
                .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `sender in fler svar enn det finnes mulighet for - skal kaste exception`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        assertThat(catchThrowable {
            validerOgOppdater(vilkårsvurdering, regel,
                              VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI),
                              VilkårSvarDto(RegelId.KRAV_SIVILSTAND, SvarId.NEI),
                              VilkårSvarDto(RegelId.KRAV_SIVILSTAND))
        })
                .hasMessageStartingWith("Finnes ikke noen flere regler, men finnes flere svar")
                .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `regelId for det andre spørsmålet er feil - skal kaste exception`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        assertThat(catchThrowable {
            validerOgOppdater(vilkårsvurdering, regel,
                              VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI),
                              VilkårSvarDto(RegelId.NÆRE_BOFORHOLD, SvarId.NEI))
        })
                .hasMessage("Finner ikke regelId=NÆRE_BOFORHOLD for vilkårType=ALENEOMSORG")
                .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `har begrunnelse på ett spørsmål som ikke skal ha begrunnelse - skal kaste exception`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        assertThat(catchThrowable {
            validerOgOppdater(vilkårsvurdering, regel,
                              VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA, "b"))
        })
                .hasMessage("Begrunnelse for vilkårType=ALENEOMSORG regelId=BOR_OG_OPPHOLDER_SEG_I_NORGE svarId=JA skal ikke ha begrunnelse")
                .isInstanceOf(Feil::class.java)
    }

    @Test
    fun `har kun svart på første spørsmål som er en sluttnode - allt ok`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA))
        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `har svart på två spørsmål hvor det siste er en sluttnode - allt ok`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "a"),
                                         VilkårSvarDto(RegelId.KRAV_SIVILSTAND, SvarId.JA))
        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `har svart på två spørsmål hvor det siste er en sluttnode men mangler begrunnelse på første`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI),
                                         VilkårSvarDto(RegelId.KRAV_SIVILSTAND, SvarId.JA))
        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `har svart på två spørsmål hvor det som gir IKKE_OPPFYLT`() {
        val regel = VilkårsregelMedEttDelvilkår()
        val vilkårsvurdering = vurderingFraDb(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "a"),
                                         VilkårSvarDto(RegelId.KRAV_SIVILSTAND, SvarId.NEI))
        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `två rotRegler - en IKKE_OPPFYLT og en OPPFYLT`() {
        val regel = VilkårsregelMedTvåRotRegler()
        val vilkårsvurdering = vurderingFraDb(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         delvilkårsvurderingDto(VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA)),
                                         delvilkårsvurderingDto(VilkårSvarDto(RegelId.KRAV_SIVILSTAND, SvarId.NEI)))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.delvilkår(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE).resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.delvilkår(RegelId.KRAV_SIVILSTAND).resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `två rotRegler - en IKKE_TATT_STILLING_TIL og en OPPFYLT`() {
        val regel = VilkårsregelMedTvåRotRegler()
        val vilkårsvurdering = vurderingFraDb(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         delvilkårsvurderingDto(
                                                 VilkårSvarDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "")),
                                         delvilkårsvurderingDto(VilkårSvarDto(RegelId.KRAV_SIVILSTAND, SvarId.JA)))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(resultat.delvilkår(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE).resultat)
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(resultat.delvilkår(RegelId.KRAV_SIVILSTAND).resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    private fun Vilkårsvurdering.delvilkår(regelId: RegelId) =
            this.delvilkårsvurdering.delvilkårsvurderinger.singleOrNull { it.rotRegelId == regelId }
            ?: error("Finner ikke regelId=$regelId blant ${this.delvilkårsvurdering.delvilkårsvurderinger.map { it.rotRegelId }}")

    private fun Vilkårsvurdering.førsteDelvilkår() = this.delvilkårsvurdering.delvilkårsvurderinger.first()

    private class VilkårsregelMedEttDelvilkår :
            Vilkårsregel(VilkårType.ALENEOMSORG,
                         setOf(RegelSteg(regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                         svarMapping = jaNeiMapping(hvisNei = NesteRegel(RegelId.KRAV_SIVILSTAND,
                                                                                         BegrunnelseType.PÅKREVD))),
                               RegelSteg(regelId = RegelId.KRAV_SIVILSTAND,
                                         svarMapping = jaNeiMapping())),
                         rotregler = setOf(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE))

    private class VilkårsregelMedTvåRotRegler :
            Vilkårsregel(VilkårType.ALENEOMSORG,
                         setOf(RegelSteg(regelId = RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                         svarMapping = jaNeiMapping(hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)),
                               RegelSteg(regelId = RegelId.KRAV_SIVILSTAND,
                                         svarMapping = jaNeiMapping())),
                         rotregler = setOf(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                           RegelId.KRAV_SIVILSTAND))

    private fun delvilkårsvurderingDto(vararg svar: VilkårSvarDto) =
            DelvilkårsvurderingDto(resultat = Vilkårsresultat.IKKE_AKTUELL, svar = svar.toList())

    private fun validerOgOppdater(vilkårsvurdering: Vilkårsvurdering,
                                  regel: Vilkårsregel,
                                  vararg vilkårSvarDto: VilkårSvarDto): Vilkårsvurdering {
        return validerOgOppdater(vilkårsvurdering, regel, delvilkårsvurderingDto(*vilkårSvarDto))
    }

    private fun validerOgOppdater(vilkårsvurdering: Vilkårsvurdering,
                                  regel: Vilkårsregel,
                                  vararg delvilkårsvurderingDto: DelvilkårsvurderingDto): Vilkårsvurdering {
        return OppdaterVilkår.validerOgOppdater(vilkårsvurdering = vilkårsvurdering,
                                                vilkårsregler = listOf(regel),
                                                oppdatering = delvilkårsvurderingDto.toList())
    }

    private fun vurderingFraDb(regel: Vilkårsregel): Vilkårsvurdering {
        val delvilkårsvurderinger = regel.rotregler.map {
            Delvilkårsvurdering(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL, svar = listOf(VilkårSvar(it)))
        }
        return Vilkårsvurdering(behandlingId = UUID.randomUUID(),
                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                type = VilkårType.ALENEOMSORG,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger))
    }


}