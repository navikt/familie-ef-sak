package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.SakMapper
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.kontrakter.ef.sak.Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*
import no.nav.familie.ef.sak.repository.Sak as Domenesak

@Service
class SakService(private val sakRepository: SakRepository,
                 private val customRepository: CustomRepository<Domenesak>) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun mottaSak(sak: Sak): UUID {

        val domenesak = SakMapper.toDomain(sak)

        val save = customRepository.persist(domenesak)
        logger.info("lagret ${save.id}")
        return save.id
    }

    fun hentSak(id: UUID): Sak {
        val sak = sakRepository.findByIdOrNull(id) ?: error("Ugyldig Primærnøkkel : $id")
        return SakMapper.toDto(sak)
    }

}
