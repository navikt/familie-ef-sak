package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.domene.Søker
import no.nav.familie.ef.sak.integration.dto.pdl.KjønnType
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class GrunnlagsdataRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var grunnlagsdataRepository: GrunnlagsdataRepository

    @Test
    internal fun `hente data går OK`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val grunnlagsdata = opprettGrunnlagsdata()
        grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdata))

        assertThat(grunnlagsdataRepository.findByIdOrThrow(behandling.id).data).isEqualTo(grunnlagsdata)
    }

    @Test
    internal fun `finnBehandlingerSomManglerGrunnlagsdata skal finne behandlinger som har status OPPRETTET`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = BehandlingStatus.OPPRETTET,
                                                                type = BehandlingType.BLANKETT))
        val behandling2 = behandlingRepository.insert(behandling(fagsak,
                                                                 status = BehandlingStatus.UTREDES,
                                                                 type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val behandling3 = behandlingRepository.insert(behandling(fagsak,
                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                 type = BehandlingType.BLANKETT))

        val behandlinger = grunnlagsdataRepository.finnBehandlingerSomManglerGrunnlagsdata()
        assertThat(behandlinger.map { it.first }).containsExactlyInAnyOrder(behandling.id, behandling2.id, behandling3.id)
        assertThat(behandlinger.find { it.first == behandling.id }!!.second.let { BehandlingStatus.valueOf(it) })
                .isEqualTo(BehandlingStatus.OPPRETTET)
    }

    private fun opprettGrunnlagsdata() = GrunnlagsdataDomene(Søker(adressebeskyttelse = null,
                                                                   bostedsadresse = emptyList(),
                                                                   dødsfall = null,
                                                                   forelderBarnRelasjon = emptyList(),
                                                                   fødsel = emptyList(),
                                                                   folkeregisterpersonstatus = emptyList(),
                                                                   fullmakt = emptyList(),
                                                                   kjønn = KjønnType.UKJENT,
                                                                   kontaktadresse = emptyList(),
                                                                   navn = Navn("", "", "", Metadata(false)),
                                                                   opphold = emptyList(),
                                                                   oppholdsadresse = emptyList(),
                                                                   sivilstand = emptyList(),
                                                                   statsborgerskap = emptyList(),
                                                                   telefonnummer = emptyList(),
                                                                   tilrettelagtKommunikasjon = emptyList(),
                                                                   innflyttingTilNorge = emptyList(),
                                                                   utflyttingFraNorge = emptyList(),
                                                                   vergemaalEllerFremtidsfullmakt = emptyList()
    ),
                                                             emptyList(),
                                                             Medlemskapsinfo("", emptyList(), emptyList(), emptyList()),
                                                             emptyList())
}