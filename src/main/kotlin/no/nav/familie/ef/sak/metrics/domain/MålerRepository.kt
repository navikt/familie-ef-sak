package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MålerRepository : CrudRepository<Behandling, UUID> {

    // language=PostgreSQL
    @Query("""SELECT COUNT(*) FROM behandling WHERE arsak = :behandlingÅrsak""")
    fun finnAntallBehandlingerAvÅrsak(behandlingÅrsak: BehandlingÅrsak): Int

    // language=PostgreSQL
    @Query("""SELECT stonadstype,
                     EXTRACT(ISOYEAR FROM behandling.opprettet_tid) AS år,
                     EXTRACT(WEEK FROM behandling.opprettet_tid) AS uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              WHERE status <> 'FERDIGSTILT'
              GROUP BY stonadstype, år, uke""")
    fun finnÅpneBehandlingerPerUke(): List<ForekomsterPerUke>

    // language=PostgreSQL
    @Query("""SELECT stonadstype, status, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              WHERE status <> 'FERDIGSTILT'
              GROUP BY stonadstype, status""")
    fun finnÅpneBehandlinger(): List<BehandlingerPerStatus>


    // language=PostgreSQL
    @Query("""SELECT stonadstype,
                     resultat,
                     EXTRACT(ISOYEAR FROM behandling.endret_tid) AS år,
                     EXTRACT(WEEK FROM behandling.endret_tid) AS uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              WHERE status = 'FERDIGSTILT'
              GROUP BY stonadstype, resultat, år, uke""")
    fun finnVedtakPerUke(): List<VedtakPerUke>

    @Query("""
        SELECT COUNT(*)
        FROM gjeldende_iverksatte_behandlinger gib
        JOIN fagsak fs ON fs.id = gib.fagsak_id
        JOIN behandling b ON b.fagsak_id = fs.id
        JOIN vedtak v ON v.behandling_id = b.id
        WHERE gib.stonadstype = 'OVERGANGSSTØNAD' AND gib.arsak='SANKSJON_1_MND'
    """)
    fun finnAntallSanksjoner(): Int


}
