package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class VedtakHelperService {
    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var beregnYtelseSteg: BeregnYtelseSteg

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    fun ferdigstillVedtak(
        vedtak: Vedtak,
        behandling: Behandling,
        fagsak: Fagsak,
    ) {
        vedtakService.lagreVedtak(vedtak.tilVedtakDto(), behandling.id, fagsak.stønadstype)

        beregnYtelseSteg.utførSteg(saksbehandling(fagsak, behandling), vedtak.tilVedtakDto())
        behandlingRepository.update(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
    }
}
