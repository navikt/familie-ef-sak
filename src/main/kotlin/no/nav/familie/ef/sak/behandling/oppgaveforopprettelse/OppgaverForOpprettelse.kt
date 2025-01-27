package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.util.UUID

data class OppgaverForOpprettelse(
    @Id
    val behandlingId: UUID,
    val oppgavetyper: List<OppgaveForOpprettelseType>,
    @Column("ar_for_inntektskontroll_selvstendig_neringsdrivende")
    val årForInntektskontrollSelvstendigNæringsdrivende: Int? = null,
)
