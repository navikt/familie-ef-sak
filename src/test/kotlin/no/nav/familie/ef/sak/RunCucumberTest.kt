package no.nav.familie.ef.sak

import io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME
import io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@SelectPackages("no.nav.familie.ef.sak")
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "no.nav.familie.ef.sak.cucumber.steps,no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps",
)
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class RunCucumberTest
