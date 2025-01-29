package no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OppgaverForFerdigstillingService(
    private val oppgaverForFerdigstillingRepository: OppgaverForFerdigstillingRepository,
) {
    @Transactional
    fun lagreOppgaveIderForFerdigstilling(
        behandlingId: UUID,
        oppgaveIder: List<Long>,
    ): OppgaverForFerdigstilling =
        when (oppgaverForFerdigstillingRepository.existsById(behandlingId)) {
            true ->
                oppgaverForFerdigstillingRepository.update(
                    OppgaverForFerdigstilling(behandlingId, oppgaveIder),
                )
            false ->
                oppgaverForFerdigstillingRepository.insert(
                    OppgaverForFerdigstilling(behandlingId, oppgaveIder),
                )
        }

    fun hentOppgaverForFerdigstillingEllerNull(behandlingId: UUID): OppgaverForFerdigstilling? = oppgaverForFerdigstillingRepository.findByIdOrNull(behandlingId)
}
