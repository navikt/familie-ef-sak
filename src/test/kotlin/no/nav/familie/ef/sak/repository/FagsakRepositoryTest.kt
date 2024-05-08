package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.testutil.hasCauseMessageContaining
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Nested
    inner class FinnFagsakerMedUtdatertGBelop {

        @Test
        fun `finnFerdigstilteEllerSattPåVentFagsakerMedUtdatertGBelop skal finne fagsak med behandling satt på vent`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("1"))))
            val behandlingFerdig = behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                    opprettetTid = LocalDateTime.now().minusDays(5),
                ),
            )
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = BehandlingStatus.SATT_PÅ_VENT,
                    resultat = BehandlingResultat.IKKE_SATT,
                    opprettetTid = LocalDateTime.now().minusDays(2),
                ),
            )
            behandlingRepository.insert(
                behandling(
                    fagsak2,
                    status = BehandlingStatus.UTREDES,
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )
            tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingFerdig.id, fagsak.personIdenter.first().ident, 2022))
            assertThat(fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(LocalDate.of(2022, 5, 1)).size).isEqualTo(0)
        }

        @Test
        fun `finner alle ferdigstilte fagsaker med innvilget tilkjent ytelse etter ny G-dato med gammel grunnbeløpsdato`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("1"))))
            val behandling = behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                    opprettetTid = LocalDateTime.now().minusDays(2),
                ),
            )
            behandlingRepository.insert(
                behandling(
                    fagsak2,
                    status = BehandlingStatus.UTREDES,
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandling.id,
                    fagsak.personIdenter.first().ident,
                    2022,
                    grunnbeløpsmåned = YearMonth.of(2021, 5),
                ),
            )
            assertThat(fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(LocalDate.of(2022, 5, 1)))
                .containsExactly(fagsak.id)
        }

        @Test
        fun `finnFerdigstilteFagsakerMedUtdatertGBelop - skal ikke finne fagsaker med samordningsfradrag`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("1"))))
            val behandling = behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                    opprettetTid = LocalDateTime.now().minusDays(2),
                ),
            )
            behandlingRepository.insert(
                behandling(
                    fagsak2,
                    status = BehandlingStatus.UTREDES,
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )
            tilkjentYtelseRepository.insert(
                tilkjentYtelse(
                    behandling.id,
                    fagsak.personIdenter.first().ident,
                    2022,
                    grunnbeløpsmåned = YearMonth.of(2021, 5),
                    samordningsfradrag = 1500,
                ),
            )
            assertThat(fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(LocalDate.of(2022, 5, 1))).isEmpty()
        }

        @Test
        fun `tar ikke med ferdigstilte fagsaker som har en åpen behandling`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("1"))))
            val behandlingFerdig = behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                    opprettetTid = LocalDateTime.now().minusDays(5),
                ),
            )
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    status = BehandlingStatus.FATTER_VEDTAK,
                    resultat = BehandlingResultat.INNVILGET,
                    opprettetTid = LocalDateTime.now().minusDays(2),
                ),
            )
            behandlingRepository.insert(
                behandling(
                    fagsak2,
                    status = BehandlingStatus.UTREDES,
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )
            tilkjentYtelseRepository.insert(tilkjentYtelse(behandlingFerdig.id, fagsak.personIdenter.first().ident, 2022))
            assertThat(fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(LocalDate.of(2022, 5, 1))).isEmpty()
        }
    }

    @Test
    fun `harLøpendeUtbetaling returnerer true for fagsak med ferdigstilt behandling med aktiv utbetaling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
            ),
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", LocalDate.now().year))

        val harLøpendeUtbetaling = fagsakRepository.harLøpendeUtbetaling(fagsak.id)

        assertThat(harLøpendeUtbetaling).isTrue()
    }

    @Test
    fun `harLøpendeUtbetaling returnerer true for fagsak med flere aktive ytelser`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
            ),
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", LocalDate.now().year))
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", LocalDate.now().year))

        val harLøpendeUtbetaling = fagsakRepository.harLøpendeUtbetaling(fagsak.id)

        assertThat(harLøpendeUtbetaling).isTrue()
    }

    @Test
    fun `harLøpendeUtbetaling returnerer false for fagsak med ferdigstilt behandling med inaktiv utbetaling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
            ),
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321"))

        val harLøpendeUtbetaling = fagsakRepository.harLøpendeUtbetaling(fagsak.id)

        assertThat(harLøpendeUtbetaling).isFalse()
    }

    @Test
    internal fun `skal ikke være mulig med flere stønader av samme typen for samme person`() {
        val person = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        StønadType.values().forEach {
            fagsakRepository.insert(fagsakDomain(personId = person.id, stønadstype = it))
        }
        StønadType.values().forEach {
            assertThatThrownBy { fagsakRepository.insert(fagsakDomain(personId = person.id, stønadstype = it)) }
                .hasRootCauseInstanceOf(PSQLException::class.java)
                .has(
                    hasCauseMessageContaining(
                        "ERROR: duplicate key value violates " +
                            "unique constraint \"fagsak_person_unique\"",
                    ),
                )
        }
    }

    @Test
    internal fun `2 ulike personer skal kunne ha samme type stønad`() {
        val person1 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("1"))))
        val person2 = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent("2"))))
        fagsakRepository.insert(fagsakDomain(personId = person1.id, stønadstype = StønadType.OVERGANGSSTØNAD))
        fagsakRepository.insert(fagsakDomain(personId = person2.id, stønadstype = StønadType.OVERGANGSSTØNAD))
    }

    @Test
    internal fun findByFagsakId() {
        val fagsakPersistert = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsak = fagsakRepository.findByIdOrNull(fagsakPersistert.id) ?: error("Finner ikke fagsak med id")

        assertThat(fagsak).isNotNull
        assertThat(fagsak.id).isEqualTo(fagsakPersistert.id)
    }

    @Test
    internal fun findBySøkerIdent() {
        testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678901", "98765432109"))))
        val fagsakHentetFinnesIkke = fagsakRepository.findBySøkerIdent(setOf("0"), StønadType.OVERGANGSSTØNAD)

        assertThat(fagsakHentetFinnesIkke).isNull()

        val fagsak = fagsakRepository.findBySøkerIdent(setOf("12345678901"), StønadType.OVERGANGSSTØNAD)
            ?: error("Finner ikke fagsak")
        val person = fagsakPersonRepository.findByIdOrThrow(fagsak.fagsakPersonId)

        assertThat(person.identer.map { it.ident }).contains("12345678901")
        assertThat(person.identer.map { it.ident }).contains("98765432109")
    }

    @Test
    internal fun `skal returnere en liste med fagsaker hvis stønadstypen ikke satt`() {
        val ident = "12345678901"
        val person = testoppsettService.opprettPerson(ident)
        val fagsak1 = testoppsettService.lagreFagsak(
            fagsak(
                person = person,
                stønadstype = StønadType.OVERGANGSSTØNAD,
            ),
        )
        val fagsak2 = testoppsettService.lagreFagsak(
            fagsak(
                person = person,
                stønadstype = StønadType.SKOLEPENGER,
            ),
        )
        val fagsaker = fagsakRepository.findBySøkerIdent(setOf(ident))

        assertThat(
            fagsaker.forEach { fagsak ->
                val fagsakperson = fagsakPersonRepository.findByIdOrThrow(fagsak.fagsakPersonId)
                assertThat(fagsakperson.identer.size).isEqualTo(1)
                assertThat(fagsakperson.identer.map { it.ident }).contains(ident)
            },
        )

        assertThat(fagsaker.map { it.stønadstype }).contains(StønadType.SKOLEPENGER)
        assertThat(fagsaker.map { it.stønadstype }).contains(StønadType.OVERGANGSSTØNAD)
        assertThat(fagsaker).containsExactlyInAnyOrder(fagsak1.tilFagsakDomain(), fagsak2.tilFagsakDomain())
    }

    @Test
    internal fun finnMedEksternId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val findByEksternId = fagsakRepository.finnMedEksternId(fagsak.eksternId)
            ?: error("Fagsak med ekstern id ${fagsak.eksternId} finnes ikke")

        assertThat(findByEksternId).isEqualTo(fagsak.tilFagsakDomain())
    }

    @Test
    internal fun `findByFagsakPersonIdAndStønadstype - skal finne fagsak`() {
        val person = testoppsettService.opprettPerson("1")
        val overgangsstønad = testoppsettService.lagreFagsak(fagsak(person = person))
        val barnetilsyn = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = StønadType.BARNETILSYN))
        testoppsettService.lagreFagsak(fagsak())

        assertThat(fagsakRepository.findByFagsakPersonIdAndStønadstype(person.id, StønadType.OVERGANGSSTØNAD)!!.id)
            .isEqualTo(overgangsstønad.id)
        assertThat(fagsakRepository.findByFagsakPersonIdAndStønadstype(person.id, StønadType.BARNETILSYN)!!.id)
            .isEqualTo(barnetilsyn.id)
        assertThat(fagsakRepository.findByFagsakPersonIdAndStønadstype(person.id, StønadType.SKOLEPENGER)).isNull()
    }

    @Test
    internal fun `finnMedEksternId skal gi null når det ikke finnes fagsak for gitt id`() {
        val findByEksternId = fagsakRepository.finnMedEksternId(100000L)
        assertThat(findByEksternId).isEqualTo(null)
    }

    @Test
    internal fun `finnAktivIdent - skal finne aktiv ident`() {
        val fagsak = opprettFagsakMedFlereIdenter()
        testoppsettService.lagreFagsak(fagsak)
        assertThat(fagsakRepository.finnAktivIdent(fagsak.id)).isEqualTo("2")
    }

    @Test
    internal fun `skal hente fagsak på behandlingId`() {
        var fagsak = opprettFagsakMedFlereIdenter()
        fagsak = testoppsettService.lagreFagsak(fagsak)
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val finnFagsakTilBehandling = fagsakRepository.finnFagsakTilBehandling(behandling.id)!!

        assertThat(finnFagsakTilBehandling.id).isEqualTo(fagsak.id)
        assertThat(finnFagsakTilBehandling.eksternId).isEqualTo(fagsak.eksternId)
    }

    @Test
    internal fun `skal sette eksternId til 200_000_000 som default`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThat(fagsak.eksternId).isGreaterThanOrEqualTo(200_000_000)
    }

    @Test
    internal fun `skal hente siste identen for hver fagsak`() {
        val fagsak = testoppsettService.lagreFagsak(opprettFagsakMedFlereIdenter())
        val fagsak2 = testoppsettService.lagreFagsak(opprettFagsakMedFlereIdenter("4", "5", "6"))
        val aktiveIdenterPerFagsak = fagsakRepository.finnAktivIdenter(setOf(fagsak.id, fagsak2.id))
        assertThat(aktiveIdenterPerFagsak).hasSize(2)
        assertThat(aktiveIdenterPerFagsak.single { it.first == fagsak.id }.second).isEqualTo("2")
        assertThat(aktiveIdenterPerFagsak.single { it.first == fagsak2.id }.second).isEqualTo("5")
    }

    @Test
    internal fun `skal kunne søke opp fagsak basert på forskjellige personidenter - kun ett treff per fagsak`() {
        val fagsakMedFlereIdenter = testoppsettService.lagreFagsak(opprettFagsakMedFlereIdenter("4", "5", "6"))

        assertThat(fagsakMedFlereIdenter.personIdenter).hasSize(3)
        assertThat(
            fagsakRepository.findBySøkerIdent(
                fagsakMedFlereIdenter.personIdenter.map { it.ident }.toSet(),
                StønadType.OVERGANGSSTØNAD,
            ),
        ).isNotNull
        assertThat(
            fagsakRepository.findBySøkerIdent(
                setOf(
                    fagsakMedFlereIdenter.personIdenter.map { it.ident }
                        .first(),
                ),
            ),
        ).hasSize(
            1,
        )
        assertThat(fagsakRepository.findBySøkerIdent(fagsakMedFlereIdenter.personIdenter.map { it.ident }.toSet())).hasSize(1)
    }

    private fun opprettFagsakMedFlereIdenter(ident: String = "1", ident2: String = "2", ident3: String = "3"): Fagsak {
        val endret2DagerSiden = Sporbar(endret = Endret(endretTid = LocalDateTime.now().plusDays(2)))
        return fagsak(
            setOf(
                PersonIdent(ident = ident),
                PersonIdent(ident = ident2, sporbar = endret2DagerSiden),
                PersonIdent(ident = ident3),
            ),
        )
    }
}
