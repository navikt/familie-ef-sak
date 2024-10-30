package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.util.opprettBarnMedIdent
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.fagsakpersonerAvPersonIdenter
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjeldendeBarnRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var gjeldendeBarnRepository: GjeldendeBarnRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var grunnlagsdataRepository: GrunnlagsdataRepository

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel, forvent barn fra behandling med fremtidig andel `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val behandlingMedTidligereAndel = lagreInnvilgetBehandling(fagsak)

        lagreHistoriskAndel(behandlingMedTidligereAndel, beløp = 1)

        val behandlingMedFremtidigAndel = lagreInnvilgetBehandling(fagsak, behandlingMedTidligereAndel)

        lagreFremtidligAndel(behandlingMedFremtidigAndel, beløp = 1)

        barnRepository.insertAll(
            listOf(
                barn(behandlingId = behandlingMedFremtidigAndel.id),
                barn(behandlingId = behandlingMedTidligereAndel.id),
            ),
        )

        val barnForUtplukk = finnBarnAvGjeldendeIverksatteBehandlinger()
        assertThat(barnForUtplukk.size).isEqualTo(1)
        assertThat(barnForUtplukk.all { !it.fraMigrering }).isTrue
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel med null i inntekt, forvent treff `() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))

        val behandlingMedFremtidigAndel = lagreInnvilgetBehandling(fagsak)

        lagreFremtidligAndel(behandlingMedFremtidigAndel, beløp = 0)

        barnRepository.insertAll(listOf(barn(behandlingId = behandlingMedFremtidigAndel.id)))

        val barnForUtplukk = finnBarnAvGjeldendeIverksatteBehandlinger()
        assertThat(barnForUtplukk.size).isEqualTo(1)
    }

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med to personidenter av samme fagsak, forvent siste opprettede personident i resultat `() {
        val nyesteFnrSøker = "12345678910"
        val eldsteFnrSøker = "12345678911"
        val fagsak =
            testoppsettService.lagreFagsak(
                fagsak(
                    identer =
                        fagsakpersonerAvPersonIdenter(
                            setOf(
                                PersonIdent(
                                    nyesteFnrSøker,
                                    Sporbar(opprettetTid = LocalDateTime.now()),
                                ),
                                PersonIdent(
                                    eldsteFnrSøker,
                                    Sporbar(opprettetTid = LocalDateTime.now().minusDays(1)),
                                ),
                            ),
                        ),
                ),
            )

        val behandlingMedFremtidigAndel = lagreInnvilgetBehandling(fagsak)
        lagreFremtidligAndel(behandlingMedFremtidigAndel, beløp = 1)

        barnRepository.insertAll(listOf(barn(behandlingId = behandlingMedFremtidigAndel.id)))

        val barnForUtplukk = finnBarnAvGjeldendeIverksatteBehandlinger()
        assertThat(barnForUtplukk.size).isEqualTo(1)
        assertThat(barnForUtplukk.first().fødselsnummerSøker).isEqualTo(nyesteFnrSøker)
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }

    @Test
    internal fun `finnBarnAvGjeldendeIverksatteBehandlinger med fremtidig andel fra to forskjellige fagsaker, forvent barn fra behandling med fremtidig andel`() {
        val fagsakForTidligereAndel = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910"))))
        val fagsakForFremtidigAndel = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678911"))))

        val behandlingMedTidligereAndel =
            behandlingRepository.insert(
                behandling(
                    fagsakForTidligereAndel,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                    opprettetTid = LocalDateTime.now().minusDays(2),
                ),
            )

        val tidligereAndel =
            lagAndelTilkjentYtelse(
                beløp = 1,
                kildeBehandlingId = behandlingMedTidligereAndel.id,
                fraOgMed = LocalDate.now().minusMonths(2),
                tilOgMed = LocalDate.now().minusMonths(1),
            )

        tilkjentYtelseRepository.insert(
            lagTilkjentYtelse(
                behandlingId = behandlingMedTidligereAndel.id,
                andelerTilkjentYtelse = listOf(tidligereAndel),
            ),
        )

        val behandlingMedFremtidigAndel =
            behandlingRepository.insert(
                behandling(
                    fagsakForFremtidigAndel,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                    opprettetTid = LocalDateTime.now().minusDays(2),
                ),
            )

        lagreFremtidligAndel(behandlingMedFremtidigAndel, beløp = 1)

        val barnListe =
            listOf(
                barn(behandlingId = behandlingMedFremtidigAndel.id, personIdent = "1"),
                barn(behandlingId = behandlingMedFremtidigAndel.id, personIdent = "2"),
                barn(behandlingId = behandlingMedTidligereAndel.id, personIdent = "3"),
            )

        barnRepository.insertAll(barnListe)
        val barnForUtplukk = finnBarnAvGjeldendeIverksatteBehandlinger()
        assertThat(barnForUtplukk.size).isEqualTo(2)
        barnForUtplukk.map {
            assertThat(it).hasNoNullFieldsOrProperties()
        }
        barnForUtplukk.forEach { assertThat(it.behandlingId).isEqualTo(behandlingMedFremtidigAndel.id) }
    }

    @Nested
    inner class FinnBarnTilMigrerteBehandlinger {
        @Test
        internal fun `skal finne barn til de som er migrert og ikke har behandlingBarn`() {
            val fnrSøker = "12345678910"
            val fnrBarn = "1"
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf(fnrSøker)), migrert = true))
            val behandling = lagreInnvilgetBehandling(fagsak)
            val grunnlagsdataDomene = opprettGrunnlagsdata().copy(barn = listOf(opprettBarnMedIdent(fnrBarn)))
            grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdataDomene))
            lagreFremtidligAndel(behandling, beløp = 1)

            assertThat(finnBarnAvGjeldendeIverksatteBehandlinger()).isEmpty()
            val resultat = finnBarnTilMigrerteBehandlinger()
            assertThat(resultat).hasSize(1)
            assertThat(resultat[0].behandlingId).isEqualTo(behandling.id)
            assertThat(resultat[0].fødselsnummerSøker).isEqualTo(fnrSøker)
            assertThat(resultat[0].fødselsnummerBarn).isEqualTo(fnrBarn)
            assertThat(resultat[0].termindatoBarn).isNull()
            assertThat(resultat[0].fraMigrering).isTrue
        }

        @Test
        internal fun `finner ikke barn når det finnes behandlingbarn på personen`() {
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("12345678910")), migrert = true))
            val behandling = lagreInnvilgetBehandling(fagsak)
            val grunnlagsdataDomene = opprettGrunnlagsdata().copy(barn = listOf(opprettBarnMedIdent("1")))
            grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdataDomene))
            lagreFremtidligAndel(behandling, beløp = 1)

            barnRepository.insert(barn(behandlingId = behandling.id, personIdent = "1"))

            assertThat(finnBarnAvGjeldendeIverksatteBehandlinger()).hasSize(1)
            assertThat(finnBarnTilMigrerteBehandlinger()).isEmpty()
        }

        @Test
        internal fun `skal ikke finne barn til de som ikke er migrert og ikke har behandlingBarn`() {
            val fnrSøker = "12345678910"
            val fnrBarn = "1"
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf(fnrSøker)), migrert = false))
            val behandling = lagreInnvilgetBehandling(fagsak)
            val grunnlagsdataDomene = opprettGrunnlagsdata().copy(barn = listOf(opprettBarnMedIdent(fnrBarn)))
            grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdataDomene))
            lagreFremtidligAndel(behandling, beløp = 1)

            assertThat(finnBarnAvGjeldendeIverksatteBehandlinger()).isEmpty()
            assertThat(finnBarnTilMigrerteBehandlinger()).isEmpty()
        }
    }

    private fun finnBarnTilMigrerteBehandlinger() = gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, LocalDate.now())

    private fun finnBarnAvGjeldendeIverksatteBehandlinger() = gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, LocalDate.now())

    private fun lagreInnvilgetBehandling(
        fagsak: Fagsak,
        tidligereBehandling: Behandling? = null,
        opprettetTid: LocalDateTime =
            tidligereBehandling?.sporbar?.opprettetTid?.plusHours(1)
                ?: LocalDateTime.now(),
    ) = behandlingRepository.insert(
        behandling(
            fagsak,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
            forrigeBehandlingId = tidligereBehandling?.id,
            opprettetTid = opprettetTid,
        ),
    )

    private fun lagreHistoriskAndel(
        behandling: Behandling,
        beløp: Int,
    ): TilkjentYtelse {
        val andel =
            lagAndelTilkjentYtelse(
                beløp = beløp,
                kildeBehandlingId = behandling.id,
                fraOgMed = LocalDate.now().minusMonths(2),
                tilOgMed = LocalDate.now().minusMonths(1),
            )
        return tilkjentYtelseRepository.insert(
            lagTilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = listOf(andel),
            ),
        )
    }

    private fun lagreFremtidligAndel(
        behandling: Behandling,
        beløp: Int,
    ): TilkjentYtelse {
        val andel =
            lagAndelTilkjentYtelse(
                beløp = beløp,
                kildeBehandlingId = behandling.id,
                fraOgMed = LocalDate.now().minusMonths(1),
                tilOgMed = LocalDate.now().plusMonths(1),
            )
        return tilkjentYtelseRepository.insert(
            lagTilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = listOf(andel),
            ),
        )
    }

    private fun barn(
        behandlingId: UUID,
        personIdent: String? = null,
        termindato: LocalDate? = LocalDate.now(),
    ): BehandlingBarn =
        BehandlingBarn(
            behandlingId = behandlingId,
            personIdent = personIdent,
            fødselTermindato = termindato,
            navn = null,
            søknadBarnId = UUID.randomUUID(),
        )
}
