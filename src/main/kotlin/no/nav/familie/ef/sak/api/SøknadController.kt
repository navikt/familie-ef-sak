package no.nav.familie.ef.sak.api

import no.nav.familie.kontrakter.ef.søknad.Søknad
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import no.nav.familie.kontrakter.felles.Ressurs


@RestController
@RequestMapping(path = ["/api/soknad"], produces = [APPLICATION_JSON_VALUE])
class SøknadController {

    @PostMapping("sendinn")
    fun sendInn(@RequestBody søknad: Søknad): ResponseEntity<Ressurs<Unit>>{
        return ResponseEntity.status(HttpStatus.CREATED).body(Ressurs.success(Unit))
    }

}
