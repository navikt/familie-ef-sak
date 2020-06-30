package no.nav.familie.ef.sak.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.annotation.Cacheable
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaMethod

internal class CachedKodeverkServiceTest {

    // for å unngå at en metode som ikke @Cacheable kaller på en metode som er @Cacheable
    @Test
    internal fun `alle publike metoder må være cacheable`() {
        assertThat(sjekkAllePublikeErCacheable(CachedKodeverkService::class)).isTrue()
    }

    @Test
    internal fun `alle publike metoder må være cacheable testklasse`() {
        open class CachedKlasse {
            @Cacheable
            open fun med() = true

            fun uten() = false
        }

        open class CachedKlasseMedPrivat {
            @Cacheable
            open fun med() = true

            private fun uten() = false
        }
        assertThat(sjekkAllePublikeErCacheable(CachedKlasse::class)).isFalse()
        assertThat(sjekkAllePublikeErCacheable(CachedKlasseMedPrivat::class)).isTrue()
    }

    private fun sjekkAllePublikeErCacheable(kClass: KClass<*>) = kClass.declaredMemberFunctions
            .filter { Modifier.isPublic(it.javaMethod!!.modifiers) }
            .filter { it.annotations.none { it.annotationClass == Cacheable::class } }.isEmpty()
}
