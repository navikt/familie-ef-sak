package no.nav.familie.ef.sak.oppfølgingsoppgave.domain

import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("oppgaver_for_opprettelse")
data class OppgaverForOpprettelse(
    @Id
    val behandlingId: UUID,
    val oppgavetyper: List<OppgaveForOpprettelseType>,
    @Column("ar_for_inntektskontroll_selvstendig_neringsdrivende")
    val årForInntektskontrollSelvstendigNæringsdrivende: Int? = null,
)
