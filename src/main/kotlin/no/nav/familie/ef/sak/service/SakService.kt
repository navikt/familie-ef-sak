package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.SakDto
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.SakMapper
import no.nav.familie.ef.sak.repository.domain.Vedlegg
import no.nav.familie.ef.sak.repository.domain.VedleggMapper
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import no.nav.familie.ef.sak.repository.domain.Sak as Domenesak

@Service
class SakService(private val sakRepository: SakRepository,
                 private val customRepository: CustomRepository<Domenesak>,
                 private val vedleggRepository: CustomRepository<Vedlegg>,
                 private val overgangsstøandService: OvergangsstøandService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun mottaSak(sak: SakRequest<SøknadOvergangsstønad>, vedleggMap: Map<String, ByteArray>): UUID {
        val domenesak = SakMapper.toDomain(sak)
        val save = customRepository.persist(domenesak)
        val vedleggListe = sak.søknad.vedlegg.map {
            val vedlegg = vedleggMap[it.id] ?: error("Finner ikke vedlegg ${it.id}")
            VedleggMapper.toDomain(save.id, it, vedlegg)
        }
        vedleggListe.forEach { vedleggRepository.persist(it) }
        logger.info("lagret ${save.id} sammen med ${vedleggListe.size} vedlegg")
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
