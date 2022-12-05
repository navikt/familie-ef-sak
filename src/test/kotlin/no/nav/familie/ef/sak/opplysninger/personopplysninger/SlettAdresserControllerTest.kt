package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class SlettAdresserControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var grunnlagsdataRepository: GrunnlagsdataRepository

    @Autowired
    private lateinit var slettAdresserController: SlettAdresserController

    private val ident = "kode6"
    private val fagsak = fagsak(identer = setOf(PersonIdent(ident)))
    private val behandling = behandling(fagsak)

    private val dato = LocalDate.now()

    @Test
    internal fun `skal slette data fra grunnlagsdata for noen identer`() {
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        lagreData(behandling)

        val opprinneligGrunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        assertThat(opprinneligGrunnlagsdata.data.søker.bostedsadresse).isNotEmpty
        assertThat(opprinneligGrunnlagsdata.data.søker.innflyttingTilNorge).isNotEmpty
        assertThat(opprinneligGrunnlagsdata.data.søker.utflyttingFraNorge).isNotEmpty

        Thread.sleep(500)
        slettAdresserController.slettData(setOf(ident))

        val oppdatertGrunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        val oppdatertSøker = oppdatertGrunnlagsdata.data.søker

        assertThat(oppdatertGrunnlagsdata.sporbar).isEqualTo(opprinneligGrunnlagsdata.sporbar)

        assertThat(oppdatertSøker.bostedsadresse).isEmpty()
        assertThat(oppdatertSøker.innflyttingTilNorge).isEmpty()
        assertThat(oppdatertSøker.utflyttingFraNorge).isEmpty()
    }

    @Test
    internal fun `skal ikke slette data fra grunnlagsdata hvis man sender inn aktørId for en person som ikke er kode 6`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("ident"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        lagreData(behandling)

        slettAdresserController.slettData(setOf(ident))

        val oppdatertGrunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        val oppdatertSøker = oppdatertGrunnlagsdata.data.søker

        assertThat(oppdatertSøker.bostedsadresse).isNotEmpty
        assertThat(oppdatertSøker.innflyttingTilNorge).isNotEmpty
        assertThat(oppdatertSøker.utflyttingFraNorge).isNotEmpty
    }

    private fun lagreData(behandling: Behandling) {
        val innflyttingTilNorge = listOf(InnflyttingTilNorge(null, null, Folkeregistermetadata(null, null)))
        val utflyttingFraNorge = listOf(UtflyttingFraNorge(null, null, null, Folkeregistermetadata(null, null)))
        grunnlagsdataRepository.insert(
            Grunnlagsdata(
                behandling.id,
                opprettGrunnlagsdata(
                    bostedsadresse = listOf(
                        Bostedsadresse(
                            angittFlyttedato = dato,
                            gyldigFraOgMed = dato,
                            gyldigTilOgMed = dato,
                            utenlandskAdresse = null,
                            coAdressenavn = null,
                            vegadresse = null,
                            ukjentBosted = null,
                            matrikkeladresse = null,
                            metadata = PdlTestdataHelper.metadataGjeldende
                        )
                    ),
                    innflyttingTilNorge = innflyttingTilNorge,
                    utflyttingFraNorge = utflyttingFraNorge
                )
            )
        )
    }
}