package no.nav.familie.ef.sak.infrastruktur.config

import efterlatte.prosessering.spring.ProsesseringLivssyklus
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.bootstrap.DefaultBootstrapContext
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.ApplicationContext
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.DefaultResourceLoader

internal class EfterlatteProsesseringConfigTest {
    @Test
    fun `dev-profilen arver ekskluderingen av etterlatte-prosessering`() {
        val environment = StandardEnvironment()
        ConfigDataEnvironmentPostProcessor.applyTo(
            environment,
            DefaultResourceLoader(),
            DefaultBootstrapContext(),
            listOf("dev"),
        )

        val exclusions =
            Binder
                .get(environment)
                .bind("spring.autoconfigure.exclude", Bindable.listOf(String::class.java))
                .get()

        assertThat(exclusions).contains("efterlatte.prosessering.spring.ProsesseringAutoConfiguration")
    }
}

/** Starterens prosesseringLivssyklus mangler @ConditionalOnMissingBean, så manglende ekskludering starter to motorer. */
internal class EfterlatteProsesseringLivssyklusTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `kun den manuelt konfigurerte etterlatte-motoren finnes`() {
        assertThat(applicationContext.getBeansOfType(ProsesseringLivssyklus::class.java))
            .containsOnlyKeys("efterlatteProsesseringLivssyklus")
    }
}
