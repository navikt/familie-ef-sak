package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.steg.VilkårSteg
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.utledBehandlingKategori
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.utledResultatForVilkårSomGjelderFlereBarn
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingStegOppdaterer(
    private val behandlingService: BehandlingService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val vilkårSteg: VilkårSteg,
    private val stegService: StegService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
) {
    fun oppdaterStegOgKategoriPåBehandling(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val lagredeVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId)

        oppdaterStegPåBehandling(saksbehandling, lagredeVilkårsvurderinger)
        oppdaterKategoriPåBehandling(saksbehandling, lagredeVilkårsvurderinger)
    }

    private fun oppdaterStegPåBehandling(
        saksbehandling: Saksbehandling,
        vilkårsvurderinger: List<Vilkårsvurdering>,
    ) {
        val vilkårsresultat =
            vilkårsvurderinger.groupBy { it.type }.map {
                if (it.key.gjelderFlereBarn()) {
                    utledResultatForVilkårSomGjelderFlereBarn(it.value)
                } else {
                    it.value.single().resultat
                }
            }

        if (saksbehandling.steg == StegType.VILKÅR && OppdaterVilkår.erAlleVilkårTattStillingTil(vilkårsresultat)) {
            stegService.håndterSteg(saksbehandling, vilkårSteg, null).id
        } else if (saksbehandling.steg != StegType.VILKÅR && vilkårsresultat.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL }) {
            stegService.resetSteg(saksbehandling.id, StegType.VILKÅR)
        } else if (saksbehandling.harStatusOpprettet) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
            behandlingshistorikkService.opprettHistorikkInnslag(behandlingId = saksbehandling.id, stegtype = StegType.VILKÅR, utfall = StegUtfall.UTREDNING_PÅBEGYNT, metadata = null)
            opprettBehandlingsstatistikkTask(saksbehandling)
        }
    }

    private fun oppdaterKategoriPåBehandling(
        saksbehandling: Saksbehandling,
        vilkårsvurderinger: List<Vilkårsvurdering>,
    ) {
        val lagretKategori = saksbehandling.kategori
        val utledetKategori = utledBehandlingKategori(vilkårsvurderinger)

        if (lagretKategori != utledetKategori) {
            behandlingService.oppdaterKategoriPåBehandling(saksbehandling.id, utledetKategori)
        }
    }

    private fun opprettBehandlingsstatistikkTask(saksbehandling: Saksbehandling) {
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = saksbehandling.id))
    }
}
