package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/revurdering"])
@ProtectedWithClaims(issuer = "azuread")
class AutomatiskRevurderingController(
    private val automatiskRevurderingService: AutomatiskRevurderingService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping("automatisk")
    fun forsøkAutomatiskRevurdering(
        @RequestBody personIdenter: List<String>,
    ): Ressurs<List<AutomatiskRevurdering>> {
        val list = mutableListOf<AutomatiskRevurdering>()
        personIdenter.forEach { personIdent ->
            val kanAutomatiskRevurderes = automatiskRevurderingService.kanAutomatiskRevurderes(personIdent)
            if (kanAutomatiskRevurderes) {
                secureLogger.info("Kan automatisk revurdere person med ident: $personIdent")
            }
            list.add(AutomatiskRevurdering(personIdent, kanAutomatiskRevurderes))
        }

        return Ressurs.success(list)
    }
}

data class AutomatiskRevurdering(
    val personIdent: String,
    val automatiskRevurdert: Boolean,
)
