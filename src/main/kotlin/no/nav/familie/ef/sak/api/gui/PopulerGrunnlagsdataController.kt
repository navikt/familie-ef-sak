package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.service.PersisterGrunnlagsdataService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/populergrunnlagsdata"])
@Unprotected
class PopulerGrunnlagsdataController (private val persisterGrunnlagsdataService: PersisterGrunnlagsdataService) {

    @GetMapping
    fun populerGrunnlagsdataTabell(): ResponseEntity<String> {
        persisterGrunnlagsdataService.populerGrunnlagsdataTabell()
        return ResponseEntity.ok("Starter hentning av grunnlagsdata...")
    }
}