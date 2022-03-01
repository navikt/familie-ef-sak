package no.nav.familie.ef.sak.patch

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@Unprotected
@RequestMapping("api/patch-aktivitet")
class PatchController(private val patchService: PatchService) {

    @GetMapping
    fun patch(@RequestParam oppdaterVedtak: Boolean = false) {
        patchService.patch(oppdaterVedtak)
    }
}

@Service
class PatchService(private val jdbcTemplate: JdbcTemplate,
                   private val behandlingService: BehandlingService,
                   private val tilkjentYtelseService: TilkjentYtelseService,
                   private val vedtakRepository: VedtakRepository,
                   private val infotrygdService: InfotrygdService) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun patch(oppdaterVedtak: Boolean) {
        val fagsaker = jdbcTemplate.query("""SELECT fagsak_id, COUNT(*)
           FROM behandling b
                    JOIN fagsak f ON b.fagsak_id = f.id
           WHERE f.migrert = TRUE
             AND resultat <> 'HENLAGT'
             AND b.type <> 'BLANKETT'
             AND resultat <> 'AVSLÅTT'
           GROUP BY fagsak_id
           HAVING COUNT(*) = 1""") { rs, _ ->
            UUID.fromString(rs.getString("fagsak_id"))
        }.toSet()

        fagsaker.forEach { fagsakId ->
            patch(fagsakId, oppdaterVedtak)
        }
    }

    private fun patch(fagsakId: UUID, oppdaterVedtak: Boolean) {
        val behandlinger = behandlingService.hentBehandlinger(fagsakId)
                .filter { it.type == BehandlingType.FØRSTEGANGSBEHANDLING || it.type == BehandlingType.REVURDERING }
                .filter { it.resultat == BehandlingResultat.INNVILGET || it.resultat == BehandlingResultat.OPPHØRT }
        if (behandlinger.size != 1) {
            logger.info("Fagsak=$fagsakId har ${behandlinger.size} behandlinger, ignorerer")
            return
        }
        val behandling = behandlinger.single()
        val behandlingId = behandling.id
        if (!behandling.erMigrering()) {
            logger.info("fagsak=$fagsakId behandling=$behandlingId er ikke en migrering")
        }
        val ty = tilkjentYtelseService.hentForBehandling(behandlingId)
        if (ty.andelerTilkjentYtelse.none { it.stønadTom > LocalDate.of(2021, 2, 27) }) {
            logger.info("fagsak=$fagsakId har ikke noen aktive andeler")
            return
        }
        val personIdent = behandlingService.hentAktivIdent(behandlingId)

        val periode = infotrygdService.hentDtoPerioder(personIdent).overgangsstønad.perioder
                .find { it.kode == InfotrygdEndringKode.OVERTFØRT_NY_LØSNING }
        if (periode == null) {
            logger.info("fagsak=$fagsakId finner ikke periode for overført til ny løsning")
            return
        }
        if (periode.aktivitetstype != InfotrygdAktivitetstype.TILMELDT_SOM_REELL_ARBEIDSSØKER) {
            logger.info("fagsak=$fagsakId er ikke reell arbeidssøker")
            return
        }
        val vedtak = vedtakRepository.findByIdOrThrow(behandlingId)
        if (vedtak.perioder?.perioder?.size != 1) {
            logger.info("fagsak=$fagsakId har ikke 1 periode")
            return
        }
        if (vedtak.perioder.perioder.single().aktivitet == AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER) {
            logger.info("fagsak=$fagsakId er allerede markert som reell arbeidssøker")
            return
        }
        val perioder1 = vedtak.perioder.perioder.map { it.copy(aktivitet = AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER) }
        val perioder = vedtak.perioder.copy(perioder = perioder1)
        logger.info("Oppdaterer fagsak=$fagsakId behandling=$behandlingId som reell arbeidssøker oppdaterVedtak=$oppdaterVedtak")
        if (oppdaterVedtak) {
            vedtakRepository.update(vedtak.copy(perioder = perioder))
        }
    }
}