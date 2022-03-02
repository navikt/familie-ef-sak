package no.nav.familie.ef.sak.patch

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Unprotected
@RequestMapping("api/patch-aktivitet")
class PatchAktivitetController(private val patchAktivitetService: PatchAktivitetService) {

    @GetMapping
    fun patch(@RequestParam oppdaterVedtak: Boolean = false) {
        patchAktivitetService.patch(oppdaterVedtak)
    }
}

@Service
class PatchAktivitetService(private val jdbcTemplate: JdbcTemplate,
                            private val behandlingService: BehandlingService,
                            private val vedtakRepository: VedtakRepository,
                            private val infotrygdService: InfotrygdService) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun patch(oppdaterVedtak: Boolean) {
        val behandlingIder = jdbcTemplate.query("""SELECT id AS behandling_id
           FROM behandling b
           WHERE b.arsak = 'MIGRERING'""") { rs, _ ->
            UUID.fromString(rs.getString("behandling_id"))
        }.toSet()

        behandlingIder.forEach { behandlingId ->
            patch(behandlingId, oppdaterVedtak)
        }
    }

    private fun patch(behandlingId: UUID, oppdaterVedtak: Boolean) {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)

        val infotrygdperioder = infotrygdService.hentDtoPerioder(personIdent).overgangsstønad.perioder
                .filter { it.kode == InfotrygdEndringKode.OVERTFØRT_NY_LØSNING }
        if (infotrygdperioder.size != 1) {
            logger.warn("behandling=$behandlingId finner ikke periode for overført til ny løsning")
            return
        }
        val periode = infotrygdperioder.single()
        if (periode.aktivitetstype != InfotrygdAktivitetstype.TILMELDT_SOM_REELL_ARBEIDSSØKER) {
            logger.info("behandling=$behandlingId er ikke reell arbeidssøker")
            return
        }
        val vedtak = vedtakRepository.findByIdOrThrow(behandlingId)
        if (vedtak.perioder?.perioder?.size != 1) {
            logger.warn("behandling=$behandlingId har ikke 1 periode")
            return
        }
        if (vedtak.perioder.perioder.single().aktivitet == AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER) {
            logger.warn("behandling=$behandlingId er allerede markert som reell arbeidssøker")
            return
        }
        val oppdatertePerioder = vedtak.perioder.perioder.map { it.copy(aktivitet = AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER) }
        logger.info("Oppdaterer behandling=$behandlingId som reell arbeidssøker oppdaterVedtak=$oppdaterVedtak")
        if (oppdaterVedtak) {
            jdbcTemplate.update("UPDATE vedtak SET perioder = ?::json WHERE behandling_id = ? ",
                                objectMapper.writeValueAsString(oppdatertePerioder),
                                behandlingId)
        }
    }
}