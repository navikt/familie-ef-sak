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
    internal fun `alle public metoder må være cacheable`() {
        assertThat(sjekkAllePublicErCacheable(CachedKodeverkService::class)).isTrue()
    }

    @Test
    internal fun `alle public metoder må være cacheable testklasse`() {
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
        assertThat(sjekkAllePublicErCacheable(CachedKlasse::class)).isFalse()
        assertThat(sjekkAllePublicErCacheable(CachedKlasseMedPrivat::class)).isTrue()
    }

    private fun sjekkAllePublicErCacheable(kClass: KClass<*>) = kClass.declaredMemberFunctions
            .filter { Modifier.isPublic(it.javaMethod!!.modifiers) }
            .none { it.annotations.none { innerIt -> innerIt.annotationClass == Cacheable::class } }
}
