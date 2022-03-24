package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface MålerRepository : CrudRepository<Behandling, UUID> {

    // language=PostgreSQL
    @Query("""SELECT COUNT(*) FROM behandling WHERE arsak = :behandlingsårsak""")
    fun finnAntallBehandlingerAvÅrsak(behandlingsårsak: BehandlingÅrsak): Int

    // language=PostgreSQL
    @Query("""SELECT b.stonadstype, dato, COUNT(*) AS antall, SUM(aty.belop) AS belop FROM gjeldende_iverksatte_behandlinger b
                JOIN tilkjent_ytelse ty ON b.id = ty.behandling_id
                JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
                JOIN GENERATE_SERIES(:fra::DATE, :til::DATE, '1 month') dato 
                  ON dato BETWEEN aty.stonad_fom AND aty.stonad_tom
              GROUP BY b.stonadstype, dato""")
    fun finnAntallLøpendeSaker(fra: LocalDate, til: LocalDate): List<LøpendeBehandling>

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

    @Query("""SELECT COUNT(*) 
              FROM behandling b
              JOIN vedtak v ON v.behandling_id = b.id
              WHERE v.resultat_type = 'SANKSJONERE'
              AND b.status = 'FERDIGSTILT'
              AND b.resultat = 'INNVILGET'""")
    fun finnAntallSanksjoner(): Int


}
