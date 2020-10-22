package no.nav.familie.ef.sak.integration.dto.pdl

data class PdlPersonRequestVariables(var ident: String)
data class PdlIdentRequestVariables(var ident: String, var gruppe: String)
data class PdlPersonBolkRequestVariables(var identer: List<String>)
