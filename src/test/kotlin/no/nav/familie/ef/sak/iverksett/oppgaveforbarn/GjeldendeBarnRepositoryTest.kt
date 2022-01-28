package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.GjeldendeBarnRepository
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class GjeldendeBarnRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var gjeldendeBarnRepository: GjeldendeBarnRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired private lateinit var søknadService: SøknadService

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel, forvent barn fra behandling med fremtidig andel `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandlingMedTidligereAndel = behandlingRepository.insert(behandling(fagsak,
                                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                                 resultat = BehandlingResultat.INNVILGET,
                                                                                 opprettetTid = LocalDateTime.now().minusDays(2)))

        val tidligereAndel = lagAndelTilkjentYtelse(beløp = 1,
                                                    kildeBehandlingId = behandlingMedTidligereAndel.id,
                                                    fraOgMed = LocalDate.now().minusMonths(2),
                                                    tilOgMed = LocalDate.now().minusMonths(1))

        tilkjentYtelseRepository.insert(lagTilkjentYtelse(behandlingId = behandlingMedTidligereAndel.id,
                                                          andelerTilkjentYtelse = listOf(tidligereAndel)))

        val behandlingMedFremtidigAndel = behandlingRepository.insert(behandling(fagsak,
                                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                                 resultat = BehandlingResultat.INNVILGET,
                                                                                 opprettetTid = LocalDateTime.now().minusDays(2)))

        val fremtidigAndel = lagAndelTilkjentYtelse(beløp = 1, kildeBehandlingId = behandlingMedFremtidigAndel.id,
                                                    fraOgMed = LocalDate.now().minusMonths(1),
                                                    tilOgMed = LocalDate.now().plusMonths(1))

        tilkjentYtelseRepository.insert(lagTilkjentYtelse(behandlingId = behandlingMedFremtidigAndel.id,
                                                          andelerTilkjentYtelse = listOf(fremtidigAndel)))

        val søknad = TestsøknadBuilder.Builder()
                .setPersonalia("Navn", FnrGenerator.generer(1985, 1, 1, false))
                .setBarn(listOf(
                        TestsøknadBuilder.Builder().defaultBarn("Barn1", fødselTermindato = LocalDate.now().plusMonths(4)),
                        TestsøknadBuilder.Builder().defaultBarn("Barn2", fødselTermindato = LocalDate.now().plusMonths(6))
                )).build().søknadOvergangsstønad

        søknadService.lagreSøknadForOvergangsstønad(søknad, behandlingMedFremtidigAndel.id, fagsak.id, "journalpostId")
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandlingMedTidligereAndel.id, fagsak.id, "journalPostId")

        val barnForUtplukk = gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(
                Stønadstype.OVERGANGSSTØNAD,
                LocalDate.now())
        assertThat(barnForUtplukk.size).isEqualTo(2)
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel fra to forskjellige fagsaker, forvent barn fra behandling med fremtidig andel`() {
        val fagsakForTidligereAndel = testoppsettService.lagreFagsak(fagsak())
        val fagsakForFremtidigAndel = testoppsettService.lagreFagsak(fagsak())

        val behandlingMedTidligereAndel = behandlingRepository.insert(behandling(fagsakForTidligereAndel,
                                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                                 resultat = BehandlingResultat.INNVILGET,
                                                                                 opprettetTid = LocalDateTime.now().minusDays(2)))

        val tidligereAndel = lagAndelTilkjentYtelse(beløp = 1,
                                                    kildeBehandlingId = behandlingMedTidligereAndel.id,
                                                    fraOgMed = LocalDate.now().minusMonths(2),
                                                    tilOgMed = LocalDate.now().minusMonths(1))

        tilkjentYtelseRepository.insert(lagTilkjentYtelse(behandlingId = behandlingMedTidligereAndel.id,
                                                          andelerTilkjentYtelse = listOf(tidligereAndel)))

        val behandlingMedFremtidigAndel = behandlingRepository.insert(behandling(fagsakForFremtidigAndel,
                                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                                 resultat = BehandlingResultat.INNVILGET,
                                                                                 opprettetTid = LocalDateTime.now().minusDays(2)))

        val fremtidigAndel = lagAndelTilkjentYtelse(beløp = 1, kildeBehandlingId = behandlingMedFremtidigAndel.id,
                                                    fraOgMed = LocalDate.now().minusMonths(1),
                                                    tilOgMed = LocalDate.now().plusMonths(1))

        tilkjentYtelseRepository.insert(lagTilkjentYtelse(behandlingId = behandlingMedFremtidigAndel.id,
                                                          andelerTilkjentYtelse = listOf(fremtidigAndel)))

        val søknad = TestsøknadBuilder.Builder()
                .setPersonalia("Navn", FnrGenerator.generer(1985, 1, 1, false))
                .setBarn(listOf(
                        TestsøknadBuilder.Builder().defaultBarn("Barn1", fødselTermindato = LocalDate.now().plusMonths(4)),
                        TestsøknadBuilder.Builder().defaultBarn("Barn2", fødselTermindato = LocalDate.now().plusMonths(6))
                )).build().søknadOvergangsstønad

        søknadService.lagreSøknadForOvergangsstønad(søknad,
                                                    behandlingMedFremtidigAndel.id,
                                                    fagsakForFremtidigAndel.id,
                                                    "journalpostId")
        søknadService.lagreSøknadForOvergangsstønad(søknad,
                                                    behandlingMedTidligereAndel.id,
                                                    fagsakForTidligereAndel.id,
                                                    "journalPostId")

        val barnForUtplukk = gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(
                Stønadstype.OVERGANGSSTØNAD,
                LocalDate.now())
        assertThat(barnForUtplukk.size).isEqualTo(2)
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }
}