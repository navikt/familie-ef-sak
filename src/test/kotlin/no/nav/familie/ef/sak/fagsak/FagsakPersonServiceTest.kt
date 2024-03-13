package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.AVSLÅTT
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.HENLAGT
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.OPPHØRT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class FagsakPersonServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var fagsakPersonService: FagsakPersonService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    internal fun `hentEllerOpprettPerson - skal kaste feil når man spør etter identer som matcher flere personer`() {
        fagsakPersonService.hentEllerOpprettPerson(setOf("1"), "1")
        fagsakPersonService.hentEllerOpprettPerson(setOf("2"), "2")
        assertThatThrownBy { fagsakPersonService.hentEllerOpprettPerson(setOf("1", "2"), "1") }
            .isInstanceOf(IncorrectResultSizeDataAccessException::class.java)
            .hasMessageContaining("Incorrect result size: expected 1, actual 2")
    }

    @Test
    internal fun `oppdaterIdent - skal oppdatere person med ny ident`() {
        val aktivIdent = "1"
        val annenIdent = "2"
        val personId = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(aktivIdent)))).id
        jdbcTemplate.update("UPDATE person_ident SET endret_tid=(endret_tid - INTERVAL '1 DAY')")

        val person = fagsakPersonRepository.findByIdOrThrow(personId)
        assertThat(person.hentAktivIdent()).isEqualTo(aktivIdent)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, annenIdent)

        assertThat(oppdatertPerson.identer.map { it.ident }).containsExactlyInAnyOrder(aktivIdent, annenIdent)
        assertThat(oppdatertPerson.hentAktivIdent()).isEqualTo(annenIdent)
    }

    @Test
    internal fun `oppdaterIdent - tidligere ident blir aktiv på nytt`() {
        val aktivIdent = "1"
        val annenIdent = "2"
        val personId =
            fagsakPersonRepository.insert(
                FagsakPerson(
                    identer =
                    setOf(
                        PersonIdent(aktivIdent),
                        PersonIdent(annenIdent),
                    ),
                ),
            ).id
        jdbcTemplate.update("UPDATE person_ident SET endret_tid=(endret_tid - INTERVAL '1 DAY') WHERE ident = '2'")

        val person = fagsakPersonRepository.findByIdOrThrow(personId)
        assertThat(person.hentAktivIdent()).isEqualTo(aktivIdent)
        val oppdatertPerson = fagsakPersonService.oppdaterIdent(person, annenIdent)

        assertThat(oppdatertPerson.identer.map { it.ident }).containsExactlyInAnyOrder(aktivIdent, annenIdent)
        assertThat(oppdatertPerson.hentAktivIdent()).isEqualTo(annenIdent)
    }

    @Test
    internal fun `hentPerson - skal hente person som finnes`() {
        val person = fagsakPersonService.hentEllerOpprettPerson(setOf("1"), "1")
        assertThat(fagsakPersonService.hentPerson(person.id)).isEqualTo(person)
    }

    @Test
    internal fun `hentPerson - skal kaste feil når person ikke finnes`() {
        assertThatThrownBy { fagsakPersonService.hentPerson(UUID.randomUUID()) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    internal fun `hentIdenter - skal kaste feil når person ikke finnes`() {
        assertThatThrownBy { fagsakPersonService.hentIdenter(UUID.randomUUID()) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Finner ikke personidenter")
    }

    @Nested
    inner class AktiveringAvMikrofrontend {
        @Test
        fun `skal finne fagsakperson gitt fagsakId`() {
            testoppsettService.opprettPerson("9")
            val fp = testoppsettService.opprettPerson("7")
            val fagsakOS = testoppsettService.lagreFagsak(fagsak(fagsakPersonId = fp.id, stønadstype = OVERGANGSSTØNAD))
            val fagsakBT = testoppsettService.lagreFagsak(fagsak(fagsakPersonId = fp.id, stønadstype = BARNETILSYN))
            val fpOS = fagsakPersonService.finnFagsakPersonForFagsakId(fagsakOS.id)
            val fpBT = fagsakPersonService.finnFagsakPersonForFagsakId(fagsakBT.id)

            assertThat(fpOS).isEqualTo(fp)
            assertThat(fpBT).isEqualTo(fp)
            assertThrows<Exception> { fagsakPersonService.finnFagsakPersonForFagsakId(UUID.randomUUID()) }
        }
    }

    @Nested
    inner class DeaktiveringAvMikrofrontend {
        val mindreEnnEnMånedSiden = LocalDateTime.now().minusMonths(1).plusDays(1)
        val merEnnEnMånedSiden = LocalDateTime.now().minusMonths(1).minusDays(1)
        val merEnnSeksMånederSiden = LocalDateTime.now().minusMonths(6).minusDays(1)
        val mindreEnnSeksMånederSiden = LocalDateTime.now().minusMonths(6).plusDays(1)
        val mindreEnnFireÅrSiden = LocalDateTime.now().minusYears(4).plusDays(1)
        val merEnnFireÅrSiden = LocalDateTime.now().minusYears(4).minusDays(1)

        @Test
        fun `skal ikke finne noen å deaktivere hvis ingen er aktive`() {
            testoppsettService.opprettPerson("12345")
            val personIderForDeaktivering = fagsakPersonService.finnFagsakpersonIderKlarForDeaktiveringAvMikrofrontend()
            assertThat(personIderForDeaktivering).isEmpty()
        }

        @Test
        fun `skal deaktivere mikrofrontend hvis fagsakperson ikke har behandlinger og er opprettet for mer enn 1 mnd siden`() {
            val forNyFagsakPerson = opprettFagsakPerson("12345", opprettetTid = mindreEnnEnMånedSiden)
            val fagsakPersonForDeaktivering = opprettFagsakPerson("54321")
            val fagsakPersonAldriAktivert = opprettFagsakPerson("543210", harAktivertMikrofrontend = false)
            opprettFagsak(fagsakPersonForDeaktivering)

            val personIderForDeaktivering = fagsakPersonService.finnFagsakpersonIderKlarForDeaktiveringAvMikrofrontend()
            assertThat(personIderForDeaktivering).containsOnly(fagsakPersonForDeaktivering.id)
        }

        @Test
        fun `skal deaktivere mikrofrontend for de med kun henlagte behandlinger og siste behandling er avsluttet for mer enn 6 mnd siden`() {
            val fagsakPersonSomSkalBeholdes = opprettFagsakPerson("12345")
            val fagsakPersonSomSkalBeholdes2 = opprettFagsakPerson("123456")
            val fagsakPersonForDeaktivering = opprettFagsakPerson("54321")

            val fagsakForDeaktivering = opprettFagsak(fagsakPersonForDeaktivering)
            val fagsakSomBeholdes = opprettFagsak(fagsakPersonSomSkalBeholdes)
            val fagsakSomBeholdes2 = opprettFagsak(fagsakPersonSomSkalBeholdes2)

            opprettBehandling(fagsakForDeaktivering, HENLAGT, merEnnSeksMånederSiden)
            opprettBehandling(fagsakSomBeholdes, HENLAGT, mindreEnnSeksMånederSiden)
            opprettBehandling(fagsakSomBeholdes2, AVSLÅTT, merEnnSeksMånederSiden)

            val personIderForDeaktivering = fagsakPersonService.finnFagsakpersonIderKlarForDeaktiveringAvMikrofrontend()

            assertThat(personIderForDeaktivering).containsOnly(fagsakPersonForDeaktivering.id)
        }

        @Test
        fun `skal deaktivere mikrofrontend for de med kun avslåtte behandlinger og siste behandling er mer enn 4 år`() {
            val fagsakPersonSomSkalBeholdes = opprettFagsakPerson("12345")
            val fagsakPersonSomSkalBeholdes2 = opprettFagsakPerson("123456")
            val fagsakPersonForDeaktivering = opprettFagsakPerson("54321")

            val fagsakForDeaktivering = opprettFagsak(fagsakPersonForDeaktivering)
            val fagsakSomBeholdes = opprettFagsak(fagsakPersonSomSkalBeholdes)
            val fagsakSomBeholdes2 = opprettFagsak(fagsakPersonSomSkalBeholdes2)

            opprettBehandling(fagsakForDeaktivering, AVSLÅTT, merEnnFireÅrSiden)
            opprettBehandling(fagsakSomBeholdes, HENLAGT, mindreEnnFireÅrSiden)
            opprettBehandling(fagsakSomBeholdes, AVSLÅTT, merEnnFireÅrSiden)
            opprettBehandling(fagsakSomBeholdes2, INNVILGET, merEnnFireÅrSiden)

            val personIderForDeaktivering = fagsakPersonService.finnFagsakpersonIderKlarForDeaktiveringAvMikrofrontend()

            assertThat(personIderForDeaktivering).containsOnly(fagsakPersonForDeaktivering.id)
        }

        @Test
        fun `skal deaktivere mikrofrontend for de med med siste utbetaling avsluttet for mer enn 4 år siden`() {
            val fagsakPersonSomSkalBeholdes = opprettFagsakPerson("12345")
            val fagsakPersonSomSkalBeholdes2 = opprettFagsakPerson("123456")
            val fagsakPersonForDeaktivering = opprettFagsakPerson("54321")

            val fagsakForDeaktivering = opprettFagsak(fagsakPersonForDeaktivering)
            val fagsakSomBeholdes = opprettFagsak(fagsakPersonSomSkalBeholdes)
            val fagsakSomBeholdes2 = opprettFagsak(fagsakPersonSomSkalBeholdes2)

            val behandlingDeaktiveresOverskrives = opprettBehandling(fagsakForDeaktivering, INNVILGET, merEnnFireÅrSiden)
            val behandlingDeaktiveresGjeldende = opprettBehandling(fagsakForDeaktivering, OPPHØRT, merEnnSeksMånederSiden)
            opprettTilkjentYtelse(behandlingDeaktiveresOverskrives, LocalDate.now().year)
            opprettTilkjentYtelse(behandlingDeaktiveresGjeldende, LocalDate.now().minusYears(5).year)

            val behandlingSomBeholdes = opprettBehandling(fagsakSomBeholdes2, INNVILGET, mindreEnnSeksMånederSiden)
            opprettTilkjentYtelse(behandlingSomBeholdes, LocalDate.now().year)

            opprettBehandling(fagsakSomBeholdes, AVSLÅTT, mindreEnnFireÅrSiden)
            opprettBehandling(fagsakSomBeholdes, AVSLÅTT, merEnnFireÅrSiden)
            opprettBehandling(fagsakSomBeholdes, HENLAGT, merEnnFireÅrSiden)

            val personIderForDeaktivering = fagsakPersonService.finnFagsakpersonIderKlarForDeaktiveringAvMikrofrontend()

            assertThat(personIderForDeaktivering).containsOnly(fagsakPersonForDeaktivering.id)
        }

        private fun opprettTilkjentYtelse(behandling: Behandling, stønadsår: Int) {
            tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "2222", stønadsår = stønadsår))
        }

        private fun opprettBehandling(
            fagsakForDeaktivering: Fagsak,
            resultat: BehandlingResultat,
            endretTid: LocalDateTime,
        ): Behandling {
            val behandling =
                behandlingRepository.insert(
                    behandling(
                        fagsakForDeaktivering,
                        resultat = resultat,
                        status = BehandlingStatus.FERDIGSTILT,
                        vedtakstidspunkt = endretTid,
                    ),
                )
            oppdaterEndretTidPåBehandling(behandling, endretTid)
            return behandling
        }

        private fun opprettFagsak(fagsakPersonForDeaktivering: FagsakPerson) =
            testoppsettService.lagreFagsak(
                fagsak(fagsakPersonId = fagsakPersonForDeaktivering.id, stønadstype = OVERGANGSSTØNAD),
            )

        private fun opprettFagsakPerson(
            ident: String,
            opprettetTid: LocalDateTime = merEnnEnMånedSiden,
            harAktivertMikrofrontend: Boolean = true,
        ) = fagsakPersonRepository.insert(
            FagsakPerson(
                identer = setOf(PersonIdent(ident)),
                opprettetTid = opprettetTid,
                harAktivertMikrofrontend = harAktivertMikrofrontend,
            ),
        )

        private fun oppdaterEndretTidPåBehandling(
            behandling: Behandling,
            endretTid: LocalDateTime,
        ) {
            jdbcTemplate.update("UPDATE behandling SET endret_tid=? where id=?", endretTid, behandling.id)
        }
    }
}
