package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.SakDto
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.SakMapper
import no.nav.familie.ef.sak.repository.domain.SakWrapper
import no.nav.familie.ef.sak.repository.domain.Vedlegg
import no.nav.familie.ef.sak.repository.domain.VedleggMapper
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
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
                 private val overgangsstønadService: OvergangsstønadService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun mottaSakOvergangsstønad(sak: SakRequest<SøknadOvergangsstønad>, vedleggMap: Map<String, ByteArray>): UUID {
        return mottaSak(SakMapper.toDomain(sak.saksnummer, sak.journalpostId, sak.søknad.søknad), sak, vedleggMap)
    }

    @Transactional
    fun mottaSakBarnetilsyn(sak: SakRequest<SøknadBarnetilsyn>, vedleggMap: Map<String, ByteArray>): UUID {
        return mottaSak(SakMapper.toDomain(sak.saksnummer, sak.journalpostId, sak.søknad.søknad), sak, vedleggMap)
    }

    private fun <T> mottaSak(domenesak: no.nav.familie.ef.sak.repository.domain.Sak,
                             sak: SakRequest<T>,
                             vedleggMap: Map<String, ByteArray>): UUID {
        val save = customRepository.persist(domenesak)
        val vedleggListe = sak.søknad.vedlegg.map {
            val vedlegg = vedleggMap[it.id] ?: error("Finner ikke vedlegg ${it.id}")
            VedleggMapper.toDomain(save.id, it, vedlegg)
        }
        vedleggListe.forEach { vedleggRepository.persist(it) }
        logger.info("lagret ${save.id} sammen med ${vedleggListe.size} vedlegg")
        return save.id
    }

    fun hentOvergangsstønad(id: UUID): SakWrapper<SøknadOvergangsstønad> {
        val sak = hentSak(id)
        return SakMapper.pakkOppOvergangsstønad(sak)
    }

    fun hentBarnetilsyn(id: UUID): SakWrapper<SøknadBarnetilsyn> {
        val sak = hentSak(id)
        return SakMapper.pakkOppBarnetisyn(sak)
    }

    fun hentSak(id: UUID): Domenesak {
        return sakRepository.findByIdOrNull(id) ?: error("Ugyldig Primærnøkkel : $id")
    }

    fun hentOvergangsstønadDto(id: UUID): SakDto {
        val sakWrapper = hentOvergangsstønad(id)
        val sak = sakWrapper.sak
        val søknad = sakWrapper.søknad
        return SakDto(id = sak.id,
                      søknad = søknad,
                      saksnummer = sak.saksnummer,
                      journalpostId = sak.journalpostId,
                      overgangsstønad = overgangsstønadService.lagOvergangsstønad(søknad))
    }

}
