package no.nav.familie.ef.sak.patch

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@Unprotected
@RequestMapping("api/patch-startdato")
class PatchStartdatoController(private val patchStardatoService: PatchStardatoService) {

    @GetMapping
    fun patch(@RequestParam oppdaterVedtak: Boolean = false) {
        patchStardatoService.patch(oppdaterVedtak)
    }
}

@Service
class PatchStardatoService(private val jdbcTemplate: JdbcTemplate,
                           private val behandlingService: BehandlingService,
                           private val vedtakRepository: VedtakRepository) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun patch(oppdaterVedtak: Boolean) {
        val vedtaksresultater = setOf(ResultatType.OPPHØRT, ResultatType.INNVILGE, ResultatType.SANKSJONERE)
        val fagsaker = jdbcTemplate.query(
                """SELECT fagsak_id FROM behandling b
                    | JOIN vedtak v ON v.behandling_id = b.id
                    | JOIN tilkjent_ytelse ty ON ty.behandling_id = b.id
                    | WHERE b.type <> 'BLANKETT' 
                    | AND v.resultat_type IN ?
                    |""".trimMargin(), { rs, _ ->
            UUID.fromString(rs.getString("fagsak_id"))
        }, vedtaksresultater).toSet()

        logger.info("Patcher ${fagsaker.size} fagsaker")
        fagsaker.forEach { fagsakId ->
            try {
                patch(fagsakId, oppdaterVedtak)
            } catch (e: Exception) {
                logger.warn("Feilet patching av $fagsakId", e)
            }
        }
        logger.info("Patch done")
    }

    fun patch(fagsakId: UUID, oppdaterVedtak: Boolean) {
        val behandlinger = behandlingService.hentBehandlinger(fagsakId)
                .filter { it.type == BehandlingType.FØRSTEGANGSBEHANDLING || it.type == BehandlingType.REVURDERING }
                .sortedBy { it.sporbar.opprettetTid }
        if (behandlinger.isEmpty()) {
            logger.info("Fagsak=$fagsakId har 0 behandlinger, ignorerer")
            return
        }

        var minStartDato: LocalDate? = null
        behandlinger.forEach { behandling ->
            val behandlingId = behandling.id
            val vedtak = vedtakRepository.findByIdOrNull(behandlingId)
            if (vedtak == null) {
                logger.info("fagsak=$fagsakId behandling=$behandlingId mangler vedtak " +
                            "status=${behandling.status} resultat=${behandling.resultat}")
                return@forEach
            }
            val vedtaksresultat = vedtak.resultatType
            val minDato = when (vedtaksresultat) {
                ResultatType.SANKSJONERE,
                ResultatType.INNVILGE -> vedtak.perioder?.perioder?.minOfOrNull { it.datoFra }
                ResultatType.OPPHØRT -> vedtak.opphørFom
                else -> {
                    logger.warn("fagsak=$fagsakId behandling=$behandlingId feil vedtaksresultat=$vedtaksresultat")
                    return@forEach
                }
            }
            if (minDato == null) {
                logger.info("fagsak=$fagsakId behandling=$behandlingId mangler minDato vedtaksresultat=$vedtaksresultat")
            }
            if (minStartDato == null || (minDato != null && minDato < minStartDato)) {
                minStartDato = minDato
            }

            if (minStartDato == null) {
                logger.warn("fagsak=$fagsakId behandling=$behandlingId mangler minStartDato")
                return // avslutter for hele fagsaken
            }

            val startdatoer =
                    jdbcTemplate.query("SELECT behandling_id, opphorsdato FROM tilkjent_ytelse WHERE behandling_id = ?",
                                       { rs, _ -> rs.getDate("opphorsdato")?.toLocalDate() },
                                       behandlingId)
            if (startdatoer.size != 1) {
                logger.info("fagsak=$fagsakId behandling=$behandlingId finner ikke tilkjent ytelse")
                return // avslutter for hele fagsaken, burde ikke kunne finne tilkjente ytelser for senere behandlinger heller.
            }
            val tilkjentYtelseStartdato = startdatoer.single()
            if (tilkjentYtelseStartdato == minStartDato) {
                logger.info("fagsak=$fagsakId behandling=$behandlingId allerede like")
                return@forEach
            }

            logger.info("fagsak=$fagsakId behandling=$behandlingId oppdaterer tilkjentYtelseStartdati=$tilkjentYtelseStartdato startdato=$minStartDato")
            if (oppdaterVedtak) {
                jdbcTemplate.update("UPDATE tilkjent_ytelse SET opphorsdato = ? WHERE behandling_id =?",
                                    minStartDato, behandlingId)
            }
        }
    }
}