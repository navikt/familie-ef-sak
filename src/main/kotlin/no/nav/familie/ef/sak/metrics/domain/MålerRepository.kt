package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.Behandling
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MålerRepository : CrudRepository<Behandling, UUID> {

    // language=PostgreSQL
    @Query("""SELECT stonadstype,
                     extract(ISOYEAR from behandling.opprettet_tid) as år,
                     extract(WEEK from behandling.opprettet_tid) as uke,
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
                     extract(ISOYEAR from behandling.endret_tid) as år,
                     extract(WEEK from behandling.endret_tid) as uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              WHERE status = 'FERDIGSTILT'
              GROUP BY stonadstype, resultat, år, uke""")
    fun finnVedtakPerUke(): List<VedtakPerUke>


}
