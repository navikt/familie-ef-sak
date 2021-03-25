package no.nav.familie.ef.sak.no.nav.familie.ef.sak.util

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.security.token.support.test.JwkGenerator
import no.nav.security.token.support.test.JwtTokenGenerator
import java.util.UUID


fun clientToken(clientId: String?, accessAsApplication: Boolean): String {
    val thisId = UUID.randomUUID().toString()
    var claimsSet = JwtTokenGenerator.createSignedJWT(clientId).jwtClaimsSet
    val builder = JWTClaimsSet.Builder(claimsSet)
            .claim("oid", thisId)
            .claim("sub", thisId)
            .claim("azp", clientId)

    if (accessAsApplication) {
        builder.claim("roles", listOf("access_as_application"))
    }

    claimsSet = builder.build()
    return JwtTokenGenerator.createSignedJWT(JwkGenerator.getDefaultRSAKey(), claimsSet).serialize()
}

fun onBehalfOfToken(role: String, saksbehandler: String = "julenissen"): String {
    val thisId = UUID.randomUUID().toString()
    val clientId = UUID.randomUUID().toString()
    var claimsSet = JwtTokenGenerator.createSignedJWT(thisId).jwtClaimsSet // default claimSet
    val builder = JWTClaimsSet.Builder(claimsSet)
            .claim("oid", saksbehandler)
            .claim("sub", thisId)
            .claim("azp", clientId)
            .claim("NAVident", saksbehandler)
            .claim("groups", listOf(role))

    claimsSet = builder.build()
    return JwtTokenGenerator.createSignedJWT(JwkGenerator.getDefaultRSAKey(), claimsSet).serialize()
}