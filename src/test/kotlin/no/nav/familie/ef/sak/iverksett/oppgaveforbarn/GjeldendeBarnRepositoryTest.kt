package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.GjeldendeBarnRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.fagsakpersonerAvPersonIdenter
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjeldendeBarnRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var gjeldendeBarnRepository: GjeldendeBarnRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired private lateinit var barnRepository: BarnRepository

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel, forvent barn fra behandling med fremtidig andel `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
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

        barnRepository.insertAll(listOf(barn(behandlingId = behandlingMedFremtidigAndel.id),
                                        barn(behandlingId = behandlingMedTidligereAndel.id)))

        val barnForUtplukk = gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(
                Stønadstype.OVERGANGSSTØNAD,
                LocalDate.now())
        assertThat(barnForUtplukk.size).isEqualTo(1)
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel med null i inntekt, forvent ingen treff `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))

        val behandlingMedFremtidigAndel = behandlingRepository.insert(behandling(fagsak,
                                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                                 resultat = BehandlingResultat.INNVILGET,
                                                                                 opprettetTid = LocalDateTime.now().minusDays(2)))

        val fremtidigAndel = lagAndelTilkjentYtelse(beløp = 0, kildeBehandlingId = behandlingMedFremtidigAndel.id,
                                                    fraOgMed = LocalDate.now().minusMonths(1),
                                                    tilOgMed = LocalDate.now().plusMonths(1))

        tilkjentYtelseRepository.insert(lagTilkjentYtelse(behandlingId = behandlingMedFremtidigAndel.id,
                                                          andelerTilkjentYtelse = listOf(fremtidigAndel)))

        barnRepository.insertAll(listOf(barn(behandlingId = behandlingMedFremtidigAndel.id)))

        val barnForUtplukk = gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(
                Stønadstype.OVERGANGSSTØNAD,
                LocalDate.now())
        assertThat(barnForUtplukk.size).isEqualTo(0)
    }

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med to personidenter av samme fagsak, forvent siste opprettede personident i resultat `() {
        val nyesteFnrSøker = "12345678910"
        val eldsteFnrSøker = "12345678911"
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersonerAvPersonIdenter(setOf(PersonIdent(
                nyesteFnrSøker, Sporbar(opprettetTid = LocalDateTime.now())), PersonIdent(
                eldsteFnrSøker, Sporbar(opprettetTid = LocalDateTime.now().minusDays(1)))))))


        val behandlingMedFremtidigAndel = behandlingRepository.insert(behandling(fagsak,
                                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                                 resultat = BehandlingResultat.INNVILGET,
                                                                                 opprettetTid = LocalDateTime.now().minusDays(2)))

        val fremtidigAndel = lagAndelTilkjentYtelse(beløp = 1, kildeBehandlingId = behandlingMedFremtidigAndel.id,
                                                    fraOgMed = LocalDate.now().minusMonths(1),
                                                    tilOgMed = LocalDate.now().plusMonths(1))

        tilkjentYtelseRepository.insert(lagTilkjentYtelse(behandlingId = behandlingMedFremtidigAndel.id,
                                                          andelerTilkjentYtelse = listOf(fremtidigAndel)))

        barnRepository.insertAll(listOf(barn(behandlingId = behandlingMedFremtidigAndel.id)))

        val barnForUtplukk = gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(
                Stønadstype.OVERGANGSSTØNAD,
                LocalDate.now())
        assertThat(barnForUtplukk.size).isEqualTo(1)
        assertThat(barnForUtplukk.first().fodselsnummerSoker).isEqualTo(nyesteFnrSøker)
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel fra to forskjellige fagsaker, forvent barn fra behandling med fremtidig andel`() {
        val fagsakForTidligereAndel = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val fagsakForFremtidigAndel = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678911"))))

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

        val barnListe = listOf(barn(behandlingId = behandlingMedFremtidigAndel.id, personIdent = "1"),
                               barn(behandlingId = behandlingMedFremtidigAndel.id, personIdent = "2"),
                               barn(behandlingId = behandlingMedTidligereAndel.id, personIdent = "3"))

        barnRepository.insertAll(barnListe)
        val barnForUtplukk = gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(
                Stønadstype.OVERGANGSSTØNAD,
                LocalDate.now())
        assertThat(barnForUtplukk.size).isEqualTo(2)
        barnForUtplukk.map {
            assertThat(it).hasNoNullFieldsOrProperties()
        }
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }

    private fun barn(behandlingId: UUID, personIdent: String? = null, termindato: LocalDate? = LocalDate.now()): BehandlingBarn {
        return BehandlingBarn(behandlingId = behandlingId,
                              personIdent = personIdent,
                              fødselTermindato = termindato,
                              navn = null,
                              søknadBarnId = UUID.randomUUID())
    }

}