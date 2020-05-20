package no.nav.familie.ba.sak.validering

import no.nav.familie.ef.sak.validering.Persontilgang
import no.nav.familie.ef.sak.validering.Persontilgang2
import no.nav.familie.ef.sak.validering.Sakstilgang
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Suppress("unused")
@MustBeDocumented
@Constraint(validatedBy = [Persontilgang::class, Persontilgang2::class])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PersontilgangConstraint(val message: String = "Ikke tilgang til person",
                                         val groups: Array<KClass<*>> = [],
                                         val payload: Array<KClass<out Payload>> = [])
