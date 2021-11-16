package no.nav.familie.ef.sak.infrastruktur.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    @Primary
    fun cacheManager(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap = Caffeine
                    .newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(60, TimeUnit.MINUTES)
                    .recordStats().build<Any, Any>().asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }

    @Bean("kodeverkCache")
    fun kodeverkCache(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap = Caffeine
                    .newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(24, TimeUnit.HOURS)
                    .recordStats().build<Any, Any>().asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }

    @Bean("shortCache")
    fun shortCache(): CacheManager = object : ConcurrentMapCacheManager() {
        override fun createConcurrentMapCache(name: String): Cache {
            val concurrentMap = Caffeine
                    .newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .recordStats().build<Any, Any>().asMap()
            return ConcurrentMapCache(name, concurrentMap, true)
        }
    }
}

/**
 * Forventer treff, skal ikke brukes hvis en cache inneholder nullverdi
 */
fun <T> CacheManager.getOrThrow(cache: String, key: String, valueLoader: () -> T) =
        (this.getCache(cache) ?: error("Finner ikke cache=$cache"))
                .get(key, valueLoader) ?: error("Finner ikke cache for cache=$cache key=$key")

fun <T> CacheManager.get(cache: String, key: String, valueLoader: () -> T?) =
        (this.getCache(cache) ?: error("Finner ikke cache=$cache")).get(key, valueLoader)
