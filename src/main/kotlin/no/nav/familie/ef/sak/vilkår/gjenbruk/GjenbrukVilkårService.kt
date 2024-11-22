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
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
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
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun finnBehandlingerForGjenbruk(behandlingId: UUID): List<BehandlingDto> {
        val fagsak: Fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val behandlingerForGjenbruk: List<Behandling> =
            behandlingService.hentBehandlingerForGjenbrukAvVilkår(fagsak.fagsakPersonId)
        val fagsaker: Map<UUID, Fagsak> =
            behandlingerForGjenbruk.map { it.fagsakId }.distinct().associateWith { fagsakService.hentFagsak(it) }
        return behandlingerForGjenbruk
            .map { it.tilDto(fagsaker.getValue(it.fagsakId).stønadstype) }
            .filterNot { it.id == behandlingId }
    }

    @Transactional
    fun gjenbrukInngangsvilkårVurderinger(
        behandlingSomSkalOppdateres: UUID,
        behandlingIdSomSkalGjenbrukeInngangsvilkår: UUID,
    ) {
        validerBehandlingForGjenbruk(
            behandlingSomSkalOppdateres,
            behandlingIdSomSkalGjenbrukeInngangsvilkår,
        )
        val vilkårsVurderingerForGjenbrukData = utledVilkårsvurderingerForGjenbrukData(
            behandlingSomSkalOppdateres,
            behandlingIdSomSkalGjenbrukeInngangsvilkår
        )
        val vurderingerSomSkalLagres =
            lagInngangsvilkårVurderingerForGjenbruk(
                behandlingSomSkalOppdateres,
                vilkårsVurderingerForGjenbrukData.nåværendeVurderinger,
                vilkårsVurderingerForGjenbrukData.tidligereVurderinger,
                vilkårsVurderingerForGjenbrukData.forrigeBarnIdTilNåværendeBarnMap,
            )
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} gjenbruker vurderinger fra behandling $behandlingIdSomSkalGjenbrukeInngangsvilkår " +
                "for å oppdatere vurderinger på inngangsvilkår for behandling $behandlingSomSkalOppdateres",
        )
        vilkårsvurderingRepository.updateAll(vurderingerSomSkalLagres)
    }

    fun hentEnkelVilkårsvurderingForGjenbruk(
        behandlingSomSkalOppdateres: UUID,
        behandlingIdSomSkalGjenbrukeInngangsvilkår: UUID,
        vilkårId: UUID,
    ): Vilkårsvurdering {
        val vilkårsVurderingerForGjenbrukData = utledVilkårsvurderingerForGjenbrukData(
            behandlingSomSkalOppdateres,
            behandlingIdSomSkalGjenbrukeInngangsvilkår
        )
        val vilkårsvurderingerForGjenbruk =
            lagInngangsvilkårVurderingerForGjenbruk(
                behandlingSomSkalOppdateres,
                vilkårsVurderingerForGjenbrukData.nåværendeVurderinger,
                vilkårsVurderingerForGjenbrukData.tidligereVurderinger,
                vilkårsVurderingerForGjenbrukData.forrigeBarnIdTilNåværendeBarnMap,
            )
        val forrigeVilkårsvurdering = vilkårsvurderingerForGjenbruk.first { it.id == vilkårId }
        return forrigeVilkårsvurdering
    }

    private fun utledVilkårsvurderingerForGjenbrukData(
        behandlingSomSkalOppdateres: UUID,
        behandlingIdSomSkalGjenbrukeInngangsvilkår: UUID
    ): VilkårsvurderingGjenbruksData {
        val forrigeBarnIdTilNåværendeBarnMap =
            finnBarnPåBeggeBehandlinger(behandlingSomSkalOppdateres, behandlingIdSomSkalGjenbrukeInngangsvilkår)
        val sivilstandErLik =
            erSivilstandUforandretSidenForrigeBehandling(behandlingSomSkalOppdateres, behandlingIdSomSkalGjenbrukeInngangsvilkår)
        val erSammeStønadstype =
            erSammeStønadstype(behandlingSomSkalOppdateres, behandlingIdSomSkalGjenbrukeInngangsvilkår)
        val tidligereVurderinger =
            hentVurderingerSomSkalGjenbrukes(
                sivilstandErLik,
                erSammeStønadstype,
                behandlingIdSomSkalGjenbrukeInngangsvilkår,
                forrigeBarnIdTilNåværendeBarnMap,
            )
        val nåværendeVurderinger =
            vilkårsvurderingRepository.findByBehandlingId(behandlingSomSkalOppdateres)

        return VilkårsvurderingGjenbruksData(
            forrigeBarnIdTilNåværendeBarnMap = forrigeBarnIdTilNåværendeBarnMap,
            sivilstandErLik = sivilstandErLik,
            erSammeStønadstype = erSammeStønadstype,
            tidligereVurderinger = tidligereVurderinger,
            nåværendeVurderinger = nåværendeVurderinger
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
            behandlingService.hentBehandlingerForGjenbrukAvVilkår(fagsak.fagsakPersonId)

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

private data class VilkårsvurderingGjenbruksData(
    val forrigeBarnIdTilNåværendeBarnMap: Map<UUID, BehandlingBarn>,
    val sivilstandErLik: Boolean,
    val erSammeStønadstype: Boolean,
    val tidligereVurderinger: List<Vilkårsvurdering>,
    val nåværendeVurderinger: List<Vilkårsvurdering>
)