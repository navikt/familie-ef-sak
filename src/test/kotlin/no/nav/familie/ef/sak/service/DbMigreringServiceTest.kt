package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.DataGenerator.tilfeldigTilkjentYtelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class DbMigreringServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired private lateinit var dbMigreringService: DbMigreringService

    @Test
    internal fun `skal oppdatere eksisterende andeler med id`() {
        val fagsak = fagsak()
        val behandling = behandling(fagsak)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        tilkjentYtelseRepository.insert(tilfeldigTilkjentYtelse(behandling, antallAndelerTilkjentYtelse = 2))
        tilkjentYtelseRepository.findAll().flatMap { it.andelerTilkjentYtelse }
                .forEach { assertThat(it.id).isNull() }

        dbMigreringService.dbMigrering()
        tilkjentYtelseRepository.findAll().flatMap { it.andelerTilkjentYtelse }
                .forEach { assertThat(it.id).isNotNull() }
    }
}