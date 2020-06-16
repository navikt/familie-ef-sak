package no.nav.familie.ef.sak.no.nav.familie.ef.sak.mapper

import org.assertj.core.api.Assertions.assertThat
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf

/**
 * Sjekker at alle verdier er satt på et objekt og att det ikke finnes noen collections som er tomme.
 * Den traverserer alle felter
 * * Når den finner ett felt som har ett packenavn som starter på kotlin. eller java. (UUID, String, Int, LocalDate) etc
 * * så sjekker den at verdiet er satt og stopper traverseringen
 */
fun sjekkAtAlleVerdierErSatt(obj: Any) {
    val kClass = obj::class
    kClass.declaredMemberProperties.forEach {
        if ((it.returnType.classifier as KClass<*>).isSubclassOf(Collection::class)) {
            val collection = it.call(obj) as Collection<*>? ?: throw IllegalStateException(lagFeilmelding(it, kClass))
            assertThat(collection).withFailMessage(lagFeilmeldingTomListe(it, kClass)).isNotEmpty
            collection.forEach { item -> sjekkAtAlleVerdierErSatt(item!!) }
        } else {
            val typeName = it.returnType.toString()
            if (typeName.startsWith("kotlin.") || typeName.startsWith("java.")) {
                assertThat(it.call(obj)).withFailMessage(lagFeilmelding(it, kClass)).isNotNull
            } else {
                sjekkAtAlleVerdierErSatt(it.call(obj) ?: throw IllegalStateException(lagFeilmelding(it, kClass)))
            }
        }
    }
}

private fun lagFeilmeldingTomListe(it: KProperty1<out Any, *>,
                                   kClass: KClass<out Any>) = "${kClass.simpleName} har en tom liste i felt ${it.name}"

private fun lagFeilmelding(it: KProperty1<out Any, *>,
                           kClass: KClass<out Any>) = "${kClass.simpleName} har ingen verdi i felt ${it.name}"
