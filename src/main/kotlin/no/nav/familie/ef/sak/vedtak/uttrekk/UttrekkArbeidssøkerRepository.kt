package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Repository
interface UttrekkArbeidssøkerRepository : RepositoryInterface<UttrekkArbeidssøkere, UUID>,
                                          InsertUpdateRepository<UttrekkArbeidssøkere> {

    fun countByÅrMånedAndKontrollertIsTrue(årMåned: YearMonth): Int

    fun findAllByÅrMåned(årMåned: YearMonth, pageable: Pageable): Page<UttrekkArbeidssøkere>

    fun findAllByÅrMånedAndKontrollertIsFalse(årMåned: YearMonth, pageable: Pageable): Page<UttrekkArbeidssøkere>

    // language=PostgreSQL
    @Query("""
        WITH q AS (
            SELECT b.id behandling_id, b.fagsak_id, ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.opprettet_tid DESC) rn
            FROM behandling b
            JOIN fagsak f ON b.fagsak_id = f.id
            WHERE
              f.stonadstype = 'OVERGANGSSTØNAD'
              AND b.type != 'BLANKETT'
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
    fun hentVedtaksperioderForSisteFerdigstilteBehandlinger(startdato: LocalDate, sluttdato: LocalDate): List<VedtaksperioderForUttrekk>
}
