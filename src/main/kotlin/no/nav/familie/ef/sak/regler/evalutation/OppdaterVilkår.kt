package no.nav.familie.ef.sak.regler.evalutation

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.svarTilDomene
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.Vilkårsregler.Companion.VILKÅRSREGLER
import no.nav.familie.ef.sak.regler.alleVilkårsregler
import no.nav.familie.ef.sak.regler.evalutation.RegelEvaluering.utledResultat
import no.nav.familie.ef.sak.regler.evalutation.RegelValidering.validerVurdering
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.service.steg.StegType
import java.util.UUID

object OppdaterVilkår {

    /**
     * Oppdaterer [Vilkårsvurdering] med nye svar og resultat
     * Validerer att svaren er gyldige
     */
    fun lagNyOppdatertVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering,
                                       oppdatering: List<DelvilkårsvurderingDto>,
                                       vilkårsregler: Map<VilkårType, Vilkårsregel> = VILKÅRSREGLER.vilkårsregler): Vilkårsvurdering {
        val vilkårsregel = vilkårsregler[vilkårsvurdering.type] ?: error("Finner ikke vilkårsregler for ${vilkårsvurdering.type}")

        validerVurdering(vilkårsregel, oppdatering, vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger)

        val vilkårsresultat = utledResultat(vilkårsregel, oppdatering)
        validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat)
        val oppdaterteDelvilkår = oppdaterDelvilkår(vilkårsvurdering, vilkårsresultat, oppdatering)
        return vilkårsvurdering.copy(resultat = vilkårsresultat.vilkår,
                                     delvilkårsvurdering = oppdaterteDelvilkår)
    }

    /**
     * Då vi ikke helt har støtte for [Vilkårsresultat.SKAL_IKKE_VURDERES] ennå så skal svaret være
     * [Vilkårsresultat.IKKE_OPPFYLT] eller [Vilkårsresultat.OPPFYLT]
     * Når man legger til funksjonalitet for SKAL_IKKE_VURDERES, hva skal resultatet være?
     */
    private fun validerAttResultatErOppfyltEllerIkkeOppfylt(vilkårsresultat: RegelResultat) {
        if (!vilkårsresultat.vilkår.oppfyltEllerIkkeOppfylt()) {
            val message = "Støtter ikke mellomlagring ennå, må håndtere ${vilkårsresultat.vilkår}. " +
                          "Ett resultat på vurderingen må bli oppfylt eller ikke oppfylt"
            throw Feil(message = message, frontendFeilmelding = message)
        }
    }

    /**
     * Oppdaterer delvilkår
     * Den beholder den opprinnelige rekkefølgen som finnes på delvilkåren i databasen, slik att frontend kan sende inn de i en annen rekkefølge
     *
     * @param vilkårsvurdering Vilkårsoppdatering fra databasen som skal oppdateres
     */
    private fun oppdaterDelvilkår(vilkårsvurdering: Vilkårsvurdering,
                                  vilkårsresultat: RegelResultat,
                                  oppdatering: List<DelvilkårsvurderingDto>): DelvilkårsvurderingWrapper {
        val vurderingerPåType = oppdatering.associateBy { it.vurderinger.first().regelId }
        val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger.map {
            if (it.resultat == Vilkårsresultat.IKKE_AKTUELL) {
                it
            } else {
                val hovedregel = it.hovedregel
                val resultat = vilkårsresultat.resultatHovedregel(hovedregel)
                val svar = vurderingerPåType[hovedregel] ?: throw Feil("Savner svar for hovedregel=$hovedregel")

                if (resultat.oppfyltEllerIkkeOppfylt()) {
                    it.copy(resultat = resultat,
                            vurderinger = svar.svarTilDomene())
                } else {
                    // TODO håndtering for [Vilkårsresultat.SKAL_IKKE_VURDERES] som burde beholde første svaret i det delvilkåret
                    throw Feil("Håndterer ikke oppdatering av resultat=$resultat ennå")
                }
            }
        }.toList()
        return vilkårsvurdering.delvilkårsvurdering.copy(delvilkårsvurderinger = delvilkårsvurderinger)
    }


    fun filtereVilkårMedResultat(lagredeVilkårsvurderinger: List<Vilkårsvurdering>,
                                 vilkårstyper: List<VilkårType>,
                                 resultat: Vilkårsresultat): List<Vilkårsvurdering> {
        val aktuelleVilkår = lagredeVilkårsvurderinger.filter { erAktuelltVilkårType(it) }

        val antallVilkårstyper = aktuelleVilkår.map { v -> v.type }.distinct().size

        require(antallVilkårstyper == vilkårstyper.size)
        { "Forventer att det er like mange vilkår som finnes definert" }

        return aktuelleVilkår.filter { it.resultat == resultat }

    }

    private fun harNoenSomSkalIkkeVurderesOgRestenErOppfylt(lagredeVilkårsvurderinger: List<Vilkårsvurdering>): Boolean {
        return lagredeVilkårsvurderinger
                .filter { erAktuelltVilkårType(it) }
                .filter { it.resultat != Vilkårsresultat.SKAL_IKKE_VURDERES }
                .all { it.resultat == Vilkårsresultat.OPPFYLT }
    }

    /* Hantering av bakåtkompatibilitet.*/
    private fun erAktuelltVilkårType(vilkårsvurdering: Vilkårsvurdering) = VilkårType.hentVilkår().contains(vilkårsvurdering.type)


    fun erAlleVilkårVurdert(behandling: Behandling,
                            lagredeVilkårsvurderinger: List<Vilkårsvurdering>,
                            vilkårstyper: List<VilkårType>): Boolean {

        if (behandling.steg == StegType.VILKÅR) {
            val harNoenVurderingIkkeTattStillingTil = filtereVilkårMedResultat(lagredeVilkårsvurderinger,
                                                                               vilkårstyper,
                                                                               Vilkårsresultat.IKKE_TATT_STILLING_TIL).isNotEmpty()
            val harNoenVurderingSkalIkkeVurderes = filtereVilkårMedResultat(lagredeVilkårsvurderinger,
                                                                            vilkårstyper,
                                                                            Vilkårsresultat.SKAL_IKKE_VURDERES).isNotEmpty()

            return when {
                harNoenVurderingIkkeTattStillingTil -> false
                harNoenVurderingSkalIkkeVurderes -> !harNoenSomSkalIkkeVurderesOgRestenErOppfylt(lagredeVilkårsvurderinger)
                else -> true
            }
        }
        return false
    }


    fun opprettNyeVilkårsvurderinger(behandlingId: UUID,
                                     metadata: HovedregelMetadata): List<Vilkårsvurdering> {
        return alleVilkårsregler
                .flatMap { vilkårsregel ->
                    if (vilkårsregel.vilkårType == VilkårType.ALENEOMSORG) {
                        metadata.søknad.barn.map {
                            lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId, it.id)
                        }
                    } else {
                        listOf(lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId))
                    }
                }
    }

    private fun lagNyVilkårsvurdering(vilkårsregel: Vilkårsregel,
                                      metadata: HovedregelMetadata,
                                      behandlingId: UUID,
                                      barnId: UUID? = null): Vilkårsvurdering {
        val delvilkårsvurdering = vilkårsregel.lagNyeDelvilkår(metadata)
        return Vilkårsvurdering(behandlingId = behandlingId,
                                type = vilkårsregel.vilkårType,
                                barnId = barnId,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering))
    }

}