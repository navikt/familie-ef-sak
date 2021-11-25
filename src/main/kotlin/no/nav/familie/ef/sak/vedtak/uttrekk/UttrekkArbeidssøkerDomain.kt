package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
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

        val kontrollert: Boolean? = null,

        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
        val sporbar: Sporbar = Sporbar()
)

/**
 * Då vedtaket som er kilden til perioden som er aktuell, så trenger vi å joine [kilde_behandling_id] fra ATY med vedtak
 */
data class ArbeidsssøkereTilUttrekk(val behandlingId: UUID,
                                    val fagsakId: UUID,
                                    val behandlingIdForVedtak: UUID,
                                    val perioder: PeriodeWrapper)