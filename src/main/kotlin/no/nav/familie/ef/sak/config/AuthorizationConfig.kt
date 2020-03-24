//package no.nav.familie.ef.sak.config
//
//import no.nav.familie.sikkerhet.AuthorizationFilter
//import no.nav.familie.sikkerhet.OIDCUtil
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//
//@Configuration
//class AuthorizationConfig(private val oidcUtil: OIDCUtil,
//                          @Value("\${ACCEPTED_CLIENTS}")
//                          private val acceptedClients: List<String>) {
// steam
//    @Bean
//    fun authorizationFilter(): AuthorizationFilter {
//        return AuthorizationFilter(oidcUtil, acceptedClients)
//    }
//}
