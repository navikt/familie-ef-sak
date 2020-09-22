package no.nav.familie.ef.sak.api.external

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.service.SakService
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.sak.Skjemasak
import no.nav.familie.kontrakter.ef.søknad.*
import no.nav.familie.util.FnrGenerator
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@RestController
@RequestMapping(path = ["/api/external/sak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ExternalSakController(private val sakService: SakService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("arbeidssoker")
    fun sendInn(@RequestBody skjemasak: Skjemasak): HttpStatus {
        // TODO
        return HttpStatus.INTERNAL_SERVER_ERROR
    }

    @PostMapping("overgangsstonad", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendInnOvergangsstønad(@RequestPart("sak") sak: SakRequest<SøknadOvergangsstønad>,
                               @RequestPart("vedlegg") vedleggListe: List<MultipartFile>): ResponseEntity<Any> {
        val vedlegg = vedleggData(vedleggListe)

        validerVedlegg(sak.søknad.vedlegg, vedlegg)
        sakService.mottaSakOvergangsstønad(sak, vedlegg)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("barnetilsyn", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendInnBarnetilsyn(@RequestPart("sak") sak: SakRequest<SøknadBarnetilsyn>,
                           @RequestPart("vedlegg") vedleggListe: List<MultipartFile>): ResponseEntity<Any> {
        val vedlegg = vedleggData(vedleggListe)

        validerVedlegg(sak.søknad.vedlegg, vedlegg)
        sakService.mottaSakBarnetilsyn(sak, vedlegg)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    private fun vedleggData(vedleggListe: List<MultipartFile>) =
            vedleggListe.map { it.originalFilename to it.bytes }.toMap()

    private fun validerVedlegg(vedlegg: List<Vedlegg>,
                               vedleggData: Map<String?, ByteArray>) {
        val vedleggMetadata = vedlegg.map { it.id to it }.toMap()
        if (vedleggMetadata.keys.size != vedleggData.keys.size || !vedleggMetadata.keys.containsAll(vedleggData.keys)) {
            logger.error("Søknad savner: [{}], vedleggListe:[{}]",
                         vedleggMetadata.keys.toMutableSet().removeAll(vedleggData.keys),
                         vedleggData.keys.toMutableSet().removeAll(vedleggMetadata.keys))
            throw ApiFeil("Savner vedlegg, se logg for mer informasjon", HttpStatus.BAD_REQUEST)
        }
    }

}
