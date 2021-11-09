package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class InnflyttingUtflyttingMapperTest {

    private val mapper = InnflyttingUtflyttingMapper(KodeverkServiceMock().kodeverkService())
    private val dato = LocalDate.of(2021, 1, 1)

    @Test
    internal fun `innflytting - skal sette dato til null hvis gyldighetstidspunkt er null`() {
        val mappet = mapper.mapInnflytting(listOf(lagInnflytting()))
        assertThat(mappet).hasSize(1)
        val mappetInnflytting = mappet[0]
        assertThat(mappetInnflytting.fraflyttingsland).isEqualTo("Sverige")
        assertThat(mappetInnflytting.fraflyttingssted).isEqualTo("Sted")
    }

    @Test
    internal fun `innflytting - skal mappe gyldighetstidspunkt til dato`() {
        val innflyttingMedGyldigshetstidspunkt = mapper.mapInnflytting(listOf(lagInnflytting(dato)))
        assertThat(innflyttingMedGyldigshetstidspunkt[0].dato).isEqualTo(dato)

        val innflyttingUtenGyldigshetstidspunkt = mapper.mapInnflytting(listOf(lagInnflytting()))
        assertThat(innflyttingUtenGyldigshetstidspunkt[0].dato).isNull()
    }

    @Test
    internal fun `innflytting - skal sortere per dato, synkende`() {
        val mappet = mapper.mapInnflytting(listOf(lagInnflytting(dato.minusDays(1)), lagInnflytting(dato), lagInnflytting()))
        assertThat(mappet).hasSize(3)
        assertThat(mappet[0].dato).isEqualTo(dato)
        assertThat(mappet[1].dato).isEqualTo(dato.minusDays(1))
        assertThat(mappet[2].dato).isNull()
    }

    @Test
    internal fun `utflytting - skal mappe alle felter`() {
        val mappet = mapper.mapUtflytting(listOf(lagUtflytting(dato)))
        assertThat(mappet).hasSize(1)
        val mappetInnflytting = mappet[0]
        assertThat(mappetInnflytting.tilflyttingsland).isEqualTo("Sverige")
        assertThat(mappetInnflytting.tilflyttingssted).isEqualTo("Sted")
        assertThat(mappetInnflytting.dato).isEqualTo(dato)
    }

    @Test
    internal fun `utflytting - skal sortere per dato, synkende`() {
        val mappet = mapper.mapUtflytting(listOf(lagUtflytting(dato.minusDays(1)), lagUtflytting(dato), lagUtflytting()))
        assertThat(mappet).hasSize(3)
        assertThat(mappet[0].dato).isEqualTo(dato)
        assertThat(mappet[1].dato).isEqualTo(dato.minusDays(1))
        assertThat(mappet[2].dato).isNull()
    }

    private fun lagInnflytting(gyldighetstidspunkt: LocalDate? = null) =
            InnflyttingTilNorge(fraflyttingsland = "SWE",
                                fraflyttingsstedIUtlandet = "Sted",
                                folkeregistermetadata = Folkeregistermetadata(
                                        gyldighetstidspunkt = gyldighetstidspunkt?.atStartOfDay(),
                                        opphørstidspunkt = LocalDateTime.now()))

    private fun lagUtflytting(utflyttingsdato: LocalDate? = null) =
            UtflyttingFraNorge(tilflyttingsland = "SWE",
                               tilflyttingsstedIUtlandet = "Sted",
                               utflyttingsdato = utflyttingsdato,
                               folkeregistermetadata = Folkeregistermetadata(gyldighetstidspunkt = null,
                                                                             opphørstidspunkt = null))
}