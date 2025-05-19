package no.nav.familie.ef.sak.vilkår.gjenbruk

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleService
import no.nav.familie.ef.sak.vilkår.BehandlingStegOppdaterer
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.dto.GjenbruktVilkårResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GjenbrukVilkårService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val barnService: BarnService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val samværsavtaleService: SamværsavtaleService,
    private val behandlingStegOppdaterer: BehandlingStegOppdaterer,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun finnBehandlingerForGjenbruk(behandlingId: UUID): List<BehandlingDto> {
        val fagsak: Fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandlingerForGjenbruk: List<Behandling> =
            behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsak.fagsakPersonId)
        val fagsaker: Map<UUID, Fagsak> =
            behandlingerForGjenbruk.map { it.fagsakId }.distinct().associateWith { fagsakService.hentFagsak(it) }
        return behandlingerForGjenbruk
            .map { it.tilDto(fagsaker.getValue(it.fagsakId).stønadstype) }
            .filterNot { it.id == behandlingId }
    }

    @Transactional
    fun gjenbrukInngangsvilkårVurderinger(
        behandlingSomSkalOppdateresId: UUID,
        behandlingForGjenbrukId: UUID,
    ) {
        validerBehandlingForGjenbruk(
            behandlingSomSkalOppdateresId,
            behandlingForGjenbrukId,
        )
        val vilkårsVurderingerForGjenbruk =
            utledGjenbrukbareVilkårsvurderinger(
                behandlingSomSkalOppdateresId,
                behandlingForGjenbrukId,
            )
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} gjenbruker vurderinger fra behandling $behandlingForGjenbrukId " +
                "for å oppdatere vurderinger på inngangsvilkår for behandling $behandlingSomSkalOppdateresId",
        )
        vilkårsvurderingRepository.updateAll(vilkårsVurderingerForGjenbruk)
    }

    @Transactional
    fun gjenbrukInngangsvilkårVurderingOgSamværsavtale(
        behandlingSomSkalOppdateresId: UUID,
        behandlingForGjenbrukId: UUID,
        vilkårSomSkalOppdateresId: UUID,
        barnPåBehandlingSomSkalOppdateres: List<BehandlingBarn>,
    ): GjenbruktVilkårResponse {
        validerBehandlingForGjenbruk(
            behandlingSomSkalOppdateresId,
            behandlingForGjenbrukId,
        )
        val vilkårsvurderingSomSkalOppdateres =
            utledGjenbrukbareVilkårsvurderinger(
                behandlingSomSkalOppdateresId,
                behandlingForGjenbrukId,
            ).first { it.id == vilkårSomSkalOppdateresId }

        if (vilkårsvurderingSomSkalOppdateres.type == VilkårType.ALENEOMSORG) {
            vilkårsvurderingSomSkalOppdateres.barnId?.let {
                samværsavtaleService.gjenbrukSamværsavtale(
                    behandlingSomSkalOppdateresId,
                    behandlingForGjenbrukId,
                    barnPåBehandlingSomSkalOppdateres,
                    vilkårsvurderingSomSkalOppdateres,
                )
            }
        }

        val samværsavtaler = samværsavtaleService.hentSamværsavtalerForBehandling(behandlingSomSkalOppdateresId)
        val gjenbruktVilkårsvurdering = gjenbrukInngangsvilkårVurdering(behandlingSomSkalOppdateresId, behandlingForGjenbrukId, vilkårsvurderingSomSkalOppdateres)

        behandlingStegOppdaterer.oppdaterStegOgKategoriPåBehandling(behandlingSomSkalOppdateresId)

        return GjenbruktVilkårResponse(gjenbruktVilkårsvurdering, samværsavtaler)
    }

    private fun gjenbrukInngangsvilkårVurdering(
        behandlingSomSkalOppdateresId: UUID,
        behandlingForGjenbrukId: UUID,
        vilkårsvurderingSomSkalOppdateres: Vilkårsvurdering,
    ): Vilkårsvurdering {
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} gjenbruker enkel vurdering fra behandling $behandlingForGjenbrukId " +
                "for å oppdatere vurderinger på inngangsvilkår for behandling $behandlingSomSkalOppdateresId",
        )
        return vilkårsvurderingRepository.update(vilkårsvurderingSomSkalOppdateres)
    }

    fun utledGjenbrukbareVilkårsvurderinger(
        behandlingSomSkalOppdateresId: UUID,
        behandlingForGjenbrukId: UUID,
    ): List<Vilkårsvurdering> {
        val forrigeBarnIdTilNåværendeBarnMap =
            finnBarnPåBeggeBehandlinger(behandlingSomSkalOppdateresId, behandlingForGjenbrukId)
        val sivilstandErLik =
            erSivilstandUforandretSidenForrigeBehandling(behandlingSomSkalOppdateresId, behandlingForGjenbrukId)
        val erSammeStønadstype =
            erSammeStønadstype(behandlingSomSkalOppdateresId, behandlingForGjenbrukId)
        val tidligereVurderinger =
            hentVurderingerSomSkalGjenbrukes(
                sivilstandErLik,
                erSammeStønadstype,
                behandlingForGjenbrukId,
                forrigeBarnIdTilNåværendeBarnMap,
            )
        val nåværendeVurderinger =
            vilkårsvurderingRepository.findByBehandlingId(behandlingSomSkalOppdateresId)

        return lagInngangsvilkårVurderingerForGjenbruk(
            behandlingSomSkalOppdateresId,
            nåværendeVurderinger,
            tidligereVurderinger,
            forrigeBarnIdTilNåværendeBarnMap,
        )
    }

    private fun erSammeStønadstype(
        nåværendeBehandlingId: UUID,
        tidligereBehandlingId: UUID,
    ): Boolean {
        val fagsak = fagsakService.hentFagsakForBehandling(nåværendeBehandlingId)
        val fagsakForTidligereBehandling = fagsakService.hentFagsakForBehandling(tidligereBehandlingId)
        return fagsak.stønadstype == fagsakForTidligereBehandling.stønadstype
    }

    private fun lagInngangsvilkårVurderingerForGjenbruk(
        behandlingId: UUID,
        nåværendeVurderinger: List<Vilkårsvurdering>,
        tidligereVurderinger: List<Vilkårsvurdering>,
        forrigeBarnIdTilNåværendeBarnMap: Map<UUID, BehandlingBarn>,
    ) = tidligereVurderinger.mapNotNull { tidligereVurdering ->
        // Dersom tidligere vurdering og matchende nåværende vurdering ikke gjelder barn vil tidligere vurdering kopieres
        val barnForVurdering = forrigeBarnIdTilNåværendeBarnMap[tidligereVurdering.barnId]
        nåværendeVurderinger
            .firstOrNull { it.type == tidligereVurdering.type && it.barnId == barnForVurdering?.id }
            ?.let { nåværendeVurdering ->
                tidligereVurdering.copy(
                    id = nåværendeVurdering.id,
                    behandlingId = behandlingId,
                    sporbar = nåværendeVurdering.sporbar,
                    barnId = nåværendeVurdering.barnId,
                    opphavsvilkår = tidligereVurdering.opprettOpphavsvilkår(),
                    delvilkårsvurdering =
                        tidligereVurdering.delvilkårsvurdering.copy(
                            delvilkårsvurderinger =
                                tidligereVurdering.gjeldendeDelvilkårsvurderinger(),
                        ),
                )
            }
    }

    private fun finnBarnPåBeggeBehandlinger(
        behandlingId: UUID,
        tidligereBehandlingId: UUID,
    ): Map<UUID, BehandlingBarn> {
        val behandlingBarn = barnService.finnBarnPåBehandling(behandlingId).associateBy { it.personIdent }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(tidligereBehandlingId)
        return barnPåForrigeBehandling
            .mapNotNull { forrige ->
                behandlingBarn[forrige.personIdent]?.let { forrige.id to it }
            }.toMap()
    }

    private fun hentVurderingerSomSkalGjenbrukes(
        sivilstandErLik: Boolean,
        erSammeStønadstype: Boolean,
        tidligereBehandlingId: UUID,
        barnPåBeggeBehandlinger: Map<UUID, BehandlingBarn>,
    ): List<Vilkårsvurdering> =
        vilkårsvurderingRepository
            .findByBehandlingId(tidligereBehandlingId)
            .filter { it.type.erInngangsvilkår() }
            .filter { skalGjenbrukeVurdering(it, sivilstandErLik, erSammeStønadstype, barnPåBeggeBehandlinger) }
            .filter { it.resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL }

    private fun erSivilstandUforandretSidenForrigeBehandling(
        behandlingId: UUID,
        tidligereBehandlingId: UUID,
    ): Boolean {
        val tidligereGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(tidligereBehandlingId)
        val nåværendeGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        return tidligereGrunnlagsdata.grunnlagsdata.søker.sivilstand
            .gjeldende() ==
            nåværendeGrunnlagsdata.grunnlagsdata.søker.sivilstand
                .gjeldende()
    }

    private fun validerBehandlingForGjenbruk(
        behandlingId: UUID,
        tidligereBehandlingId: UUID,
    ) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og vilkår kan ikke oppdateres på behandling med id=$behandlingId"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)) {
            "Behandling med id=$behandlingId eies av noen andre og vilkår kan derfor ikke oppdateres av deg"
        }

        val fagsak: Fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandlingerForGjenbruk: List<Behandling> =
            behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsak.fagsakPersonId)

        if (behandlingerForGjenbruk.isEmpty()) {
            throw Feil("Fant ingen tidligere behandlinger som kan benyttes til gjenbruk av inngangsvilkår for behandling med id=$behandlingId")
        }
        if (!behandlingerForGjenbruk.map { it.id }.contains(tidligereBehandlingId)) {
            throw Feil("Behandling med id=$tidligereBehandlingId kan ikke benyttes til gjenbruk av inngangsvilkår for behandling med id=$behandlingId")
        }
    }

    private fun skalGjenbrukeVurdering(
        vurdering: Vilkårsvurdering,
        sivilstandErLik: Boolean,
        erSammeStønadstype: Boolean,
        barnPåBeggeBehandlinger: Map<UUID, BehandlingBarn>,
    ): Boolean =
        when (vurdering.type) {
            VilkårType.SIVILSTAND -> sivilstandErLik
            VilkårType.ALENEOMSORG -> barnPåBeggeBehandlinger.containsKey(vurdering.barnId) && (erSammeStønadstype || vurdering.resultat != Vilkårsresultat.SKAL_IKKE_VURDERES)
            else -> true
        }
}
