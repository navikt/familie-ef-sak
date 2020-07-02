package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.SakDto
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.SakMapper
import no.nav.familie.ef.sak.repository.domain.Vedlegg
import no.nav.familie.ef.sak.repository.domain.VedleggMapper
import no.nav.familie.kontrakter.ef.sak.SakRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*
import no.nav.familie.ef.sak.repository.domain.Sak as Domenesak

@Service
class SakService(private val sakRepository: SakRepository,
                 private val customRepository: CustomRepository<Domenesak>,
                 private val vedleggRepository: CustomRepository<Vedlegg>,
                 private val overgangsstøandService: OvergangsstøandService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun mottaSak(sak: SakRequest): UUID {
        val domenesak = SakMapper.toDomain(sak)
        val save = customRepository.persist(domenesak)
        val vedlegg = sak.søknad.vedlegg.map { VedleggMapper.toDomain(save.id, it) }
        vedlegg.forEach { vedleggRepository.persist(it) }
        logger.info("lagret ${save.id} sammen med ${vedlegg.size} vedlegg")
        return save.id
    }

    fun hentSak(id: UUID): Domenesak {
        return sakRepository.findByIdOrNull(id) ?: error("Ugyldig Primærnøkkel : $id")
    }

    fun hentSakDto(id: UUID): SakDto {
        val sak = hentSak(id)
        return SakDto(id = sak.id,
                      søknad = sak.søknad,
                      saksnummer = sak.saksnummer,
                      journalpostId = sak.journalpostId,
                      overgangsstønad = overgangsstøandService.lagOvergangsstønad(sak.søknad))
    }

}
