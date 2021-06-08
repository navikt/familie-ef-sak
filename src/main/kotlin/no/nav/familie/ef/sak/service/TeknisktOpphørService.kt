package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository;
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TeknisktOpphørService(val behandlingService: BehandlingService,
                            val behandlingRepository: BehandlingRepository,
                            val vedtakRepository: VedtakRepository,
                            val fagsakService: FagsakService,
                            val tilkjentYtelseService: TilkjentYtelseService,
                            val vilkårsvurderingRepository: VilkårsvurderingRepository) {

    fun håndterTeknisktOpphør(personIdent: PersonIdent) {
        val sisteBehandling = behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf(personIdent.ident))
        require(sisteBehandling != null) { throw Feil("Finner ikke behandling med stønadstype overgangsstønad for personen") }
        val fagsakId = sisteBehandling.fagsakId
        val fagsakEksternFagsakId = fagsakService.hentEksternId(fagsakId)
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val nyBehandling = opprettBehandlingTekniskOpphør(fagsakId)
        val nyBehandlingId = nyBehandling.id
        opprettVurderinger(sisteBehandling.id, nyBehandlingId)
        opprettVedtak(nyBehandlingId)
        opprettTilkjentYtelse(eksternBehandlingId = nyBehandling.eksternId.id,
                              behandlingId = nyBehandlingId,
                              personIdent = aktivIdent,
                              eksternFagsakId = fagsakEksternFagsakId)
    }

    private fun opprettTilkjentYtelse(eksternFagsakId: Long, behandlingId: UUID, personIdent: String, eksternBehandlingId: Long) {
        TilkjentYtelseMedMetaData(tilkjentYtelse = TilkjentYtelse(behandlingId = behandlingId,
                                                                  personident = personIdent,
                                                                  utbetalingsoppdrag = null,
                                                                  vedtaksdato = LocalDate.now(),
                                                                  status = TilkjentYtelseStatus.OPPRETTET,
                                                                  type = TilkjentYtelseType.OPPHØR,
                                                                  andelerTilkjentYtelse = emptyList()),
                                  eksternBehandlingId = eksternBehandlingId,
                                  stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                  eksternFagsakId = eksternFagsakId)
    }

    private fun opprettBehandlingTekniskOpphør(fagsakId: UUID): Behandling {
        return behandlingService.opprettBehandling(behandlingType = BehandlingType.TEKNISK_OPPHØR, fagsakId = fagsakId)
    }

    private fun opprettVedtak(nyBehandlingId: UUID) {
        vedtakRepository.insert(Vedtak(behandlingId = nyBehandlingId,
                                       avslåBegrunnelse = "Tekniskt opphør",
                                       resultatType = ResultatType.AVSLÅ))
    }

    // TODO: skal vi mappe over delvilkårsvurderinger og sette de til noe?
    private fun opprettVurderinger(sisteBehandligId: UUID,
                                   nyBehandlingId: UUID) {
        val vilkår = vilkårsvurderingRepository.findByBehandlingId(sisteBehandligId)
        vilkår.map {
            Vilkårsvurdering(behandlingId = nyBehandlingId,
                             resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                             delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()),
                             type = it.type,
                             barnId = it.barnId)
        }.forEach { vilkårsvurderingRepository.insert(it) }
    }
}