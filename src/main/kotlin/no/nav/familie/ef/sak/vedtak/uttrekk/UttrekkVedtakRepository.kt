package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Repository
interface UttrekkVedtakRepository : RepositoryInterface<UttrekkArbeidssøkere, UUID>, InsertUpdateRepository<UttrekkArbeidssøkere> {

    // language=PostgreSQL
    @Query("""
        WITH q AS (
            SELECT b.id behandling_id, b.fagsak_id, ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.opprettet_tid DESC) rn
            FROM behandling b
            JOIN fagsak f ON b.fagsak_id = f.id
            WHERE
              b.type != 'BLANKETT'
              AND b.resultat IN ('OPPHØRT', 'INNVILGET')
              AND b.status = 'FERDIGSTILT'
            )
        SELECT DISTINCT ON (v.behandling_id) -- Trenger ikke å hente samme vedtak flere ganger 
               q1.behandling_id, q1.fagsak_id, v.behandling_id behandling_id_for_vedtak, v.perioder 
          FROM q q1
          JOIN tilkjent_ytelse ty ON ty.behandling_id = q1.behandling_id
          JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
          JOIN vedtak v ON v.behandling_id = aty.kilde_behandling_id
        WHERE aty.stonad_tom >= :startdato AND aty.stonad_fom <= :sluttdato
          AND rn = 1
    """)
    fun hentArbeidssøkere(startdato: LocalDate, sluttdato: LocalDate): List<ArbeidsssøkereTilUttrekk>
}

@Table("uttrekk_arbeidssoker")
data class UttrekkArbeidssøkere(
        @Id
        val id: UUID = UUID.randomUUID(),
        val fagsakId: UUID,
        val vedtakId: UUID,
        @Column("maaned_aar")
        val månedÅr: YearMonth,

        @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
        val kontrollert: Konntrollert? = null
)

data class Konntrollert(
        @LastModifiedBy
        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
        val endret: Endret = Endret(),

        val sjekket: Boolean
)

/**
 * Då vedtaket som er kilden til perioden som er aktuell, så trenger vi å joine [kilde_behandling_id] fra ATY med vedtak
 */
data class ArbeidsssøkereTilUttrekk(val behandlingId: UUID,
                                    val fagsakId: UUID,
                                    val behandlingIdForVedtak: UUID,
                                    val perioder: PeriodeWrapper)
