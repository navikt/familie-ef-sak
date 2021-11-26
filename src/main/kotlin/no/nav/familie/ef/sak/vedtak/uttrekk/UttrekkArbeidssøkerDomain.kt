package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Table("uttrekk_arbeidssoker")
data class UttrekkArbeidssøkere(
        @Id
        val id: UUID = UUID.randomUUID(),
        val fagsakId: UUID,
        val vedtakId: UUID,
        @Column("aar_maaned")
        val årMåned: YearMonth,
        val opprettetTid: LocalDateTime = SporbarUtils.now(),
        val kontrollert: Boolean = false,
        val kontrollertTid: LocalDateTime? = null,
        val kontrollertAv: String? = null
) {

    fun medKontrollert(kontrollert: Boolean): UttrekkArbeidssøkere {
        return this.copy(kontrollert = kontrollert,
                         kontrollertTid = SporbarUtils.now(),
                         kontrollertAv = SikkerhetContext.hentSaksbehandler())
    }
}

/**
 * Då vedtaket som er kilden til perioden som er aktuell, så trenger vi å joine [kilde_behandling_id] fra ATY med vedtak
 */
data class ArbeidsssøkereTilUttrekk(val behandlingId: UUID,
                                    val fagsakId: UUID,
                                    val behandlingIdForVedtak: UUID,
                                    val perioder: PeriodeWrapper)