package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Repository
interface UttrekkArbeidssøkerRepository : RepositoryInterface<UttrekkArbeidssøkere, UUID>,
                                          InsertUpdateRepository<UttrekkArbeidssøkere> {

    fun findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(årMåned: YearMonth): List<UttrekkArbeidssøkere>

    fun existsByÅrMånedAndFagsakId(årMåned: YearMonth, fagsakId : UUID) : Boolean

    // language=PostgreSQL
    @Query("""
        SELECT DISTINCT ON (v.behandling_id) -- Trenger ikke å hente samme vedtak flere ganger 
               gib.id behandling_id, gib.fagsak_id, v.behandling_id behandling_id_for_vedtak, v.perioder 
          FROM gjeldende_iverksatte_behandlinger gib
          JOIN tilkjent_ytelse ty ON ty.behandling_id = gib.id
          JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
          JOIN vedtak v ON v.behandling_id = aty.kilde_behandling_id
        WHERE aty.stonad_tom >= :startdato AND aty.stonad_fom <= :sluttdato
          AND gib.stonadstype = 'OVERGANGSSTØNAD'
    """)
    fun hentVedtaksperioderForSisteFerdigstilteBehandlinger(startdato: LocalDate,
                                                            sluttdato: LocalDate): List<VedtaksperioderForUttrekk>
}
