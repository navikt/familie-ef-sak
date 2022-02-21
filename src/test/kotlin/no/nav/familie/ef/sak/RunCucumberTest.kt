package no.nav.familie.ef.sak.no.nav.familie.ef.sak

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

@RunWith(Cucumber::class)
@CucumberOptions(
        features = ["src/test/resources/økonomi/features"],
        plugin = ["pretty", "html:build/cucumber.html"],
        tags = "not @ignored",
        monochrome = false
)
class RunCucumberTest
