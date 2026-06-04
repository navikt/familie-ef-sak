package no.nav.familie.ef.sak.felles.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant

object BrukerContextUtil {
    fun clearBrukerContext() {
        SecurityContextHolder.clearContext()
        RequestContextHolder.resetRequestAttributes()
    }

    fun mockBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        servletRequest: HttpServletRequest = MockHttpServletRequest(),
        azp_name: String? = null,
    ) {
        val claims =
            mutableMapOf<String, Any>(
                "preferred_username" to preferredUsername,
                "NAVident" to preferredUsername,
                "name" to preferredUsername,
                "groups" to groups,
            )
        if (azp_name != null) claims["azp_name"] = azp_name

        val jwt =
            Jwt
                .withTokenValue("mock-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims { it.putAll(claims) }
                .build()

        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(servletRequest))
    }

    fun <T> testWithBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        fn: () -> T,
    ): T {
        try {
            mockBrukerContext(preferredUsername, groups)
            return fn()
        } finally {
            clearBrukerContext()
        }
    }
}
