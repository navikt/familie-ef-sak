package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.dto.VilkårDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkårsvurderinger
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VurderingService(
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val barnService: BarnService,
    private val vilkårGrunnlagService: VilkårGrunnlagService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val fagsakService: FagsakService
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun hentEllerOpprettVurderinger(behandlingId: UUID): VilkårDto {
        val (grunnlag, metadata) = hentGrunnlagOgMetadata(behandlingId)
        val vurderinger = hentEllerOpprettVurderinger(behandlingId, metadata)
        return VilkårDto(vurderinger = vurderinger, grunnlag = grunnlag)
    }

    @Transactional
    fun oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandlingId: UUID): VilkårDto {
        grunnlagsdataService.oppdaterOgHentNyGrunnlagsdata(behandlingId)
        return hentEllerOpprettVurderinger(behandlingId)
    }

    @Transactional
    fun opprettVilkårForMigrering(behandling: Behandling) {
        feilHvisIkke(behandling.erMigrering()) { "Kan kun opprette maskinellt opprettede vurderinger på migreringer" }
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) { "Behandling er låst for videre redigering" }
        feilHvis(vilkårsvurderingRepository.findByBehandlingId(behandling.id).isNotEmpty()) { "Vilkår finnes allerede" }
        val (_, metadata) = hentGrunnlagOgMetadata(behandling.id)
        val stønadstype = fagsakService.hentFagsakForBehandling(behandling.id).stønadstype

        val nyeVilkårsvurderinger = opprettNyeVilkårsvurderinger(
            behandlingId = behandling.id,
            metadata = metadata.copy(erMigrering = true),
            stønadstype = stønadstype
        )
            .map { it.copy(resultat = Vilkårsresultat.OPPFYLT) }
        vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)
        nyeVilkårsvurderinger.forEach {
            vilkårsvurderingRepository.settMaskinelltOpprettet(it.id)
        }
    }

    @Transactional
    fun opprettVilkårForOmregning(behandling: Behandling) {
        feilHvisIkke(behandling.årsak == BehandlingÅrsak.G_OMREGNING) { "Maskinelle vurderinger kun for G-omregning." }
        val (_, metadata) = hentGrunnlagOgMetadata(behandling.id)
        val stønadstype = fagsakService.hentFagsakForBehandling(behandling.id).stønadstype
        kopierVurderingerTilNyBehandling(
            eksisterendeBehandlingId = behandling.forrigeBehandlingId ?: error("Finner ikke forrige behandlingId"),
            nyBehandlingsId = behandling.id,
            metadata = metadata,
            stønadType = stønadstype

        )
    }

    fun hentGrunnlagOgMetadata(behandlingId: UUID): Pair<VilkårGrunnlagDto, HovedregelMetadata> {
        val søknad = søknadService.hentSøknadsgrunnlag(behandlingId)
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val barn = barnService.finnBarnPåBehandling(behandlingId)
        val grunnlag = vilkårGrunnlagService.hentGrunnlag(behandlingId, søknad, personIdent, barn)
        val søktOmBarnetilsyn =
            grunnlag.barnMedSamvær.filter { it.barnepass?.skalHaBarnepass == true }.map { it.barnId }
        val metadata = HovedregelMetadata(
            sivilstandstype = grunnlag.sivilstand.registergrunnlag.type,
            sivilstandSøknad = søknad?.sivilstand,
            barn = barn,
            søktOmBarnetilsyn = søktOmBarnetilsyn
        )
        return Pair(grunnlag, metadata)
    }

    private fun hentEllerOpprettVurderinger(
        behandlingId: UUID,
        metadata: HovedregelMetadata
    ): List<VilkårsvurderingDto> {
        return hentEllerOpprettVurderingerForVilkår(behandlingId, metadata).map(Vilkårsvurdering::tilDto)
    }

    private fun hentEllerOpprettVurderingerForVilkår(
        behandlingId: UUID,
        metadata: HovedregelMetadata
    ): List<Vilkårsvurdering> {
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        return when {
            behandlingErLåstForVidereRedigering(behandlingId) -> lagredeVilkårsvurderinger
            lagredeVilkårsvurderinger.isEmpty() -> lagreNyeVilkårsvurderinger(behandlingId, metadata)
            else -> lagredeVilkårsvurderinger
        }
    }

    private fun lagreNyeVilkårsvurderinger(
        behandlingId: UUID,
        metadata: HovedregelMetadata
    ): List<Vilkårsvurdering> {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val nyeVilkårsvurderinger: List<Vilkårsvurdering> = opprettNyeVilkårsvurderinger(
            behandlingId = behandlingId,
            metadata = metadata,
            stønadstype = stønadstype
        )
        return vilkårsvurderingRepository.insertAll(nyeVilkårsvurderinger)
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: UUID) =
        behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()

    /**
     * Når en revurdering opprettes skal den kopiere de tidligere vilkårsvurderingene med lik verdi for endretTid.
     * Endret tid blir satt av Sporbar() som alltid vil sette endretTid til nåværende tispunkt, noe som blir feil.
     * For å omgå dette problemet lagres først de kopierte vilkårsvurderingene til databasen. Til slutt
     * vil oppdaterEndretTid() manuelt overskrive verdiene for endretTid til korrekte verdier.
     */
    fun kopierVurderingerTilNyBehandling(
        eksisterendeBehandlingId: UUID,
        nyBehandlingsId: UUID,
        metadata: HovedregelMetadata,
        stønadType: StønadType
    ) {
        val tidligereVurderinger =
            vilkårsvurderingRepository.findByBehandlingId(eksisterendeBehandlingId).associateBy { it.id }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(eksisterendeBehandlingId)
        val barnIdMap = byggBarnMapFraTidligereTilNyId(barnPåForrigeBehandling, metadata.barn)
        validerAtVurderingerKanKopieres(tidligereVurderinger, eksisterendeBehandlingId)

        val kopiAvVurderinger: Map<UUID, Vilkårsvurdering> = lagKopiAvTidligereVurderinger(
            tidligereVurderinger,
            metadata.barn,
            nyBehandlingsId,
            barnIdMap
        )

        val nyeBarnVurderinger = opprettVurderingerForNyeBarn(kopiAvVurderinger, metadata, stønadType)

        vilkårsvurderingRepository.insertAll(kopiAvVurderinger.values.toList() + nyeBarnVurderinger)
        tilbakestillEndretTidForKopierteVurderinger(kopiAvVurderinger, tidligereVurderinger)
    }

    @Transactional
    fun gjenbrukInngangsvilkårVurderinger(
        nåværendeBehandlingId: UUID,
        tidligereBehandlingId: UUID
    ) {
        validerBehandlingForGjenbruk(
            nåværendeBehandlingId,
            tidligereBehandlingId
        )
        val barnPåBeggeBehandlingerMap =
            finnBarnPåBeggeBehandlinger(nåværendeBehandlingId, tidligereBehandlingId)
        val sivilstandErLik =
            hentGrunnlagsdataOgValiderSivilstand(nåværendeBehandlingId, tidligereBehandlingId)
        val tidligereVurderinger = hentVurderingerSomSkalGjenbrukes(
            sivilstandErLik,
            tidligereBehandlingId,
            barnPåBeggeBehandlingerMap
        )
        val nåværendeVurderinger =
            vilkårsvurderingRepository.findByBehandlingId(nåværendeBehandlingId)
        val vurderingerSomSkalLagres = lagInngangsvilkårVurderingerForGjenbruk(
            nåværendeBehandlingId,
            nåværendeVurderinger,
            tidligereVurderinger,
            barnPåBeggeBehandlingerMap
        )
        secureLogger.info(
            "${SikkerhetContext.hentSaksbehandler()} gjenbruker vurderinger fra behandling $tidligereBehandlingId " +
                "for å oppdatere vurderinger på inngangsvilkår for behandling $nåværendeBehandlingId"
        )
        vilkårsvurderingRepository.updateAll(vurderingerSomSkalLagres)
    }

    private fun lagInngangsvilkårVurderingerForGjenbruk(
        behandlingId: UUID,
        nåværendeVurderinger: List<Vilkårsvurdering>,
        tidligereVurderinger: List<Vilkårsvurdering>,
        forrigeVurderingIdTilNåværendeBarnMap: Map<UUID, BehandlingBarn>,
    ) = tidligereVurderinger.mapNotNull { tidligereVurdering ->
        val barnForVurdering = forrigeVurderingIdTilNåværendeBarnMap[tidligereVurdering.barnId]
        nåværendeVurderinger.firstOrNull { it.type == tidligereVurdering.type && it.barnId == barnForVurdering?.id }
            ?.let {
                tidligereVurdering.copy(
                    id = it.id,
                    behandlingId = behandlingId,
                    sporbar = it.sporbar,
                    barnId = it.barnId
                )
            }
    }

    private fun finnBarnPåBeggeBehandlinger(
        behandlingId: UUID,
        tidligereBehandlingId: UUID
    ): Map<UUID, BehandlingBarn> {
        val behandlingBarn = barnService.finnBarnPåBehandling(behandlingId).associateBy { it.personIdent }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(tidligereBehandlingId)

        return barnPåForrigeBehandling.mapNotNull { forrige ->
            val barnPåBeggeBehandlinger = behandlingBarn[forrige.personIdent] ?: return@mapNotNull null
            barnPåBeggeBehandlinger.id to barnPåBeggeBehandlinger
        }.toMap()
    }

    private fun hentVurderingerSomSkalGjenbrukes(
        sivilstandErLik: Boolean,
        tidligereBehandlingId: UUID,
        barnPåBeggeBehandlinger: Map<UUID, BehandlingBarn>,
    ): List<Vilkårsvurdering> {
        val tidligereVurderinger =
            vilkårsvurderingRepository.findByBehandlingId(tidligereBehandlingId)
                .filter {
                    val skalGjenbrukeVurdering = skalGjenbrukeVurdering(
                        it,
                        sivilstandErLik,
                        barnPåBeggeBehandlinger
                    )
                    it.type.erInngangsvilkår() && skalGjenbrukeVurdering
                }
        return tidligereVurderinger
    }

    private fun hentGrunnlagsdataOgValiderSivilstand(
        behandlingId: UUID,
        tidligereBehandlingId: UUID
    ): Boolean {
        val tidligereGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(tidligereBehandlingId)
        val nåværendeGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        return tidligereGrunnlagsdata.grunnlagsdata.søker.sivilstand.gjeldende() == nåværendeGrunnlagsdata.grunnlagsdata.søker.sivilstand.gjeldende()
    }

    fun aktivitetArbeidForBehandlingIds(behandlingIds: Collection<UUID>): Map<UUID, SvarId?> {
        val vilkårsvurderinger =
            vilkårsvurderingRepository.findByTypeAndBehandlingIdIn(VilkårType.AKTIVITET_ARBEID, behandlingIds)

        return vilkårsvurderinger.associate { vilkårsvurdering ->
            val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger

            vilkårsvurdering.behandlingId to delvilkårsvurderinger.map { delvilkårsvurdering ->
                delvilkårsvurdering.vurderinger.single { it.regelId == RegelId.ER_I_ARBEID_ELLER_FORBIGÅENDE_SYKDOM }.svar
            }.single()
        }
    }

    private fun validerBehandlingForGjenbruk(
        nåværendeBehandlingId: UUID,
        tidligereBehandlingId: UUID,
    ) {
        val fagsak: Fagsak = fagsakService.hentFagsakForBehandling(nåværendeBehandlingId)
        val behandlingerForGjenbruk: List<Behandling> =
            behandlingService.hentBehandlingForGjenbrukAvVilkår(fagsak.fagsakPersonId)

        if (behandlingerForGjenbruk.isEmpty()) {
            throw Feil("Fant ingen tidligere behandlinger som kan benyttes til gjenbruk av inngangsvilkår for behandling med id=${nåværendeBehandlingId}")
        }
        if (!behandlingerForGjenbruk.map { it.id }.contains(tidligereBehandlingId)) {
            throw Feil("Behandling med id=${tidligereBehandlingId} kan ikke benyttes til gjenbruk av inngangsvilkår for behandling med id=${nåværendeBehandlingId}")
        }
    }

    private fun skalGjenbrukeVurdering(
        vurdering: Vilkårsvurdering,
        sivilstandErLik: Boolean,
        barnPåBeggeBehandlinger: Map<UUID, BehandlingBarn>
    ): Boolean {
        return when (vurdering.type) {
            VilkårType.SIVILSTAND -> sivilstandErLik
            VilkårType.ALENEOMSORG -> barnPåBeggeBehandlinger.containsKey(vurdering.barnId)
            else -> true
        }
    }

    private fun validerAtVurderingerKanKopieres(
        tidligereVurderinger: Map<UUID, Vilkårsvurdering>,
        eksisterendeBehandlingId: UUID
    ) {
        if (tidligereVurderinger.isEmpty()) {
            val melding = "Tidligere behandling=$eksisterendeBehandlingId har ikke noen vilkår"
            throw Feil(melding, melding)
        }
    }

    private fun lagKopiAvTidligereVurderinger(
        tidligereVurderinger: Map<UUID, Vilkårsvurdering>,
        barnPåGjeldendeBehandling: List<BehandlingBarn>,
        nyBehandlingsId: UUID,
        barnIdMap: Map<UUID, BehandlingBarn>
    ) =
        tidligereVurderinger.values
            .filter { skalKopiereVurdering(it, barnPåGjeldendeBehandling.isNotEmpty()) }
            .associate { vurdering ->
                vurdering.id to vurdering.copy(
                    id = UUID.randomUUID(),
                    behandlingId = nyBehandlingsId,
                    sporbar = Sporbar(),
                    barnId = finnBarnId(vurdering.barnId, barnIdMap)
                )
            }

    private fun opprettVurderingerForNyeBarn(
        vurderingerKopi: Map<UUID, Vilkårsvurdering>,
        metadata: HovedregelMetadata,
        stønadType: StønadType
    ) =
        metadata.barn
            .filter { barn -> vurderingerKopi.none { it.value.barnId == barn.id } }
            .map { OppdaterVilkår.lagVilkårsvurderingForNyttBarn(metadata, it.behandlingId, it.id, stønadType) }
            .flatten()

    private fun tilbakestillEndretTidForKopierteVurderinger(
        vurderinger: Map<UUID, Vilkårsvurdering>,
        tidligereVurderinger: Map<UUID, Vilkårsvurdering>
    ) {
        vurderinger.forEach { (forrigeId, vurdering) ->
            vilkårsvurderingRepository.oppdaterEndretTid(
                vurdering.id,
                tidligereVurderinger.getValue(forrigeId).sporbar.endret.endretTid
            )
        }
    }

    private fun finnBarnId(barnId: UUID?, barnIdMap: Map<UUID, BehandlingBarn>): UUID? {
        return barnId?.let {
            barnIdMap[it]?.id ?: error("Fant ikke barn=$it på gjeldende behandling")
        }
    }

    private fun skalKopiereVurdering(
        it: Vilkårsvurdering,
        harNyeBarnForVurdering: Boolean
    ) =
        if (it.type.gjelderFlereBarn() && it.barnId == null) {
            !harNyeBarnForVurdering
        } else {
            true
        }

    fun erAlleVilkårOppfylt(behandlingId: UUID): Boolean {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val lagredeVilkårsvurderinger: List<Vilkårsvurdering> =
            vilkårsvurderingRepository.findByBehandlingId(behandlingId)
        return OppdaterVilkår.erAlleVilkårsvurderingerOppfylt(lagredeVilkårsvurderinger, stønadstype)
    }

    companion object {

        fun byggBarnMapFraTidligereTilNyId(
            barnPåForrigeBehandling: List<BehandlingBarn>,
            barnPåGjeldendeBehandling: List<BehandlingBarn>
        ): Map<UUID, BehandlingBarn> {
            val barnFraForrigeBehandlingMap = barnPåForrigeBehandling.associateBy { it.id }.toMutableMap()
            return barnPåGjeldendeBehandling.mapNotNull { nyttBarn ->
                val forrigeBarnId =
                    barnFraForrigeBehandlingMap.entries.firstOrNull { nyttBarn.erMatchendeBarn(it.value) }?.key
                barnFraForrigeBehandlingMap.remove(forrigeBarnId)
                forrigeBarnId?.let { Pair(forrigeBarnId, nyttBarn) }
            }.associate { it.first to it.second }
        }
    }
}
