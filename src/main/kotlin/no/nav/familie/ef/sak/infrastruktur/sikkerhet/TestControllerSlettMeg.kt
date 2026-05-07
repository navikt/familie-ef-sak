package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.restklient.client.AbstractRestClient
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestOperations
import java.net.URI

@RestController
@RequestMapping("/api/forvaltning/iverksett/test/mjau")
@ProtectedWithClaims(issuer = "azuread")
class TestControllerSlettMeg(
    @Value("\${FAMILIE_EF_IVERKSETT_URL}")
    private val familieEfIverksettUri: String,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie.ef.iverksett.test") {

    private val base = "$familieEfIverksettUri/api/test/mjau"

    @GetMapping("/med-roller")
    fun testMedRoller(): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(getForEntity(URI.create("$base/med-roller")))

    @GetMapping("/med-beslutter")
    fun testMedBeslutter(): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(getForEntity(URI.create("$base/med-beslutter")))

    @GetMapping("/med-saksbehandler")
    fun testMedSaksbehandler(): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(getForEntity(URI.create("$base/med-saksbehandler")))

    @GetMapping("/med-application")
    fun testMedApplikasjon(): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(getForEntity(URI.create("$base/med-application")))

    @GetMapping("/uauthenticated")
    fun testUtenAuth(): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(getForEntity(URI.create("$base/uauthenticated")))

    @GetMapping("/fra-ef-sak-med-roller")
    fun testFraEfSak(): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(getForEntity(URI.create("$base/fra-ef-sak-med-roller")))

    @GetMapping("/hvem-er-du")
    fun hvemErDu(): ResponseEntity<Ressurs<String>> =
        ResponseEntity.ok(getForEntity(URI.create("$base/hvem-er-du")))
}