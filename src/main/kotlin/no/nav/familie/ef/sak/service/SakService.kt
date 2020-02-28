package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.kontrakter.ef.sak.Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*
import no.nav.familie.ef.sak.repository.Sak as DomeneSak

@Service
class SakService(private val sakRepository: SakRepository) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun mottaSak(sak: Sak): UUID {

        val domenesak = DomeneSak(søknad = jacksonObjectMapper().writeValueAsBytes(sak.søknad),
                                  saksnummer = sak.saksnummer,
                                  journalpostId = sak.journalpostId)

        val save = sakRepository.save(domenesak)
        logger.info("lagret ${save.id}")
        return save.id!!
    }

    fun hentSak(id: UUID): DomeneSak {
        return sakRepository.findByIdOrNull(id) ?: error("Ugyldig Primærnøkkel : $id")

    }


}
