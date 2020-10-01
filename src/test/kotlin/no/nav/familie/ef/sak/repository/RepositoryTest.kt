package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.CustomRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Endret
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.time.LocalDateTime

@ActiveProfiles("integrasjonstest")
internal class RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var customRepository: CustomRepository

    @Test
    internal fun `skal oppdatere sporbar automatisk når en entitet oppdateres`() {
        val opprinneligEndret = Endret(endretAv = "~", endretTid = LocalDateTime.MIN)
        val fagsak = customRepository.persist(fagsak(sporbar = Sporbar(endret = opprinneligEndret)))
        val opprettetSporbar = fagsakRepository.findByIdOrThrow(fagsak.id).sporbar
        val opprettetTid = fagsak.sporbar.opprettetTid
        val opprettetAv = "VL"

        // verifiser att opprettetAv og opprettetTid ikke er mulig å sette
        // kanskje ikke ønskelig men nå oppfattes alle våre entiteter som nye då vi har satt ID på de
        assertThat(opprettetSporbar.endret.endretAv).isNotEqualTo(opprinneligEndret.endretAv)
        assertThat(opprettetSporbar.endret.endretTid).isNotEqualTo(opprinneligEndret.endretTid)

        assertThat(opprettetSporbar.opprettetAv).isEqualTo(opprettetAv)
        assertThat(opprettetSporbar.opprettetTid).isEqualTo(opprettetTid)
        assertThat(opprettetSporbar.endret.endretAv).isEqualTo(opprettetAv)
        assertThat(opprettetSporbar.endret.endretTid).isEqualTo(fagsak.sporbar.endret.endretTid)

        val nyEndretAv = "En annen brukere"
        mockBrukerContext(nyEndretAv)
        val oppdatertFagsak = fagsakRepository.save(fagsak)
        val oppdatertSporbar = fagsakRepository.findByIdOrThrow(fagsak.id).sporbar

        assertThat(oppdatertSporbar.endret.endretTid).isNotEqualTo(opprettetSporbar.endret.endretTid)
        assertThat(oppdatertSporbar.opprettetAv).isEqualTo(opprettetAv)
        assertThat(oppdatertSporbar.opprettetTid).isEqualTo(opprettetTid)
        assertThat(oppdatertSporbar.endret.endretAv).isEqualTo(nyEndretAv)
        assertThat(oppdatertSporbar.endret.endretTid).isEqualTo(oppdatertFagsak.sporbar.endret.endretTid)
    }

    private fun mockBrukerContext(preferredUsername: String) {
        val tokenValidationContext = mockk<TokenValidationContext>()
        val jwtTokenClaims = mockk<JwtTokenClaims>()
        val requestAttributes = mockk<RequestAttributes>()
        RequestContextHolder.setRequestAttributes(requestAttributes)
        every {
            requestAttributes.getAttribute(SpringTokenValidationContextHolder::class.java.name,
                                           RequestAttributes.SCOPE_REQUEST)
        } returns tokenValidationContext
        every { tokenValidationContext.getClaims("azuread") } returns jwtTokenClaims
        every { jwtTokenClaims.get("preferred_username") } returns preferredUsername
    }
}
