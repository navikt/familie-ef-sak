package no.nav.familie.ef.sak.opplysninger.personopplysninger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Endring
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Endringer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.EndringerIPersonopplysningerDto
import no.nav.familie.ef.sak.repository.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class EndringerIPersonOpplysningerServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val personopplysningerService = mockk<PersonopplysningerService>()

    val service =
        EndringerIPersonOpplysningerService(behandlingService, grunnlagsdataService, personopplysningerService)

    val behandling = saksbehandling()
    val behandlingId = behandling.id

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns behandling
        every { grunnlagsdataService.hentFraRegister(behandlingId) } returns
            grunnlagsdata(LocalDateTime.now()).tilGrunnlagsdataMedMetadata()
        every { grunnlagsdataService.oppdaterEndringer(any()) } answers { firstArg() }
        mockPersonopplysninger(harEndringer = true)
    }

    @Nested
    inner class EndringerMedSjekkLengreEnn4TimerSiden {

        val femTimerSiden = LocalDateTime.now().minusHours(5)

        @BeforeEach
        internal fun setUp() {
            mockHentLagretGrunnlagsdata(grunnlagsdata(femTimerSiden, endringer = null))
        }

        @Test
        internal fun `skal sette endringer til null når det ikke finnes noen oppdateringer`() {
            mockPersonopplysninger(harEndringer = false)

            val endringer = service.hentEndringerPersonopplysninger(behandlingId)

            assertThat(endringer.endringer.harEndringer).isFalse

            verify(exactly = 1) { grunnlagsdataService.hentFraRegister(any()) }
            verify(exactly = 1) { personopplysningerService.finnEndringerIPersonopplysninger(any(), any(), any()) }
            verify(exactly = 1) {
                grunnlagsdataService.oppdaterEndringer(coWithArg {
                    assertThat(it.oppdaterteData).isNull()
                    assertThat(it.oppdaterteDataHentetTid).isAfter(LocalDateTime.now().minusMinutes(1))
                })
            }
        }

        @Test
        internal fun `skal oppdatere endringer det finnes oppdateringer`() {
            val endringer = service.hentEndringerPersonopplysninger(behandlingId)

            assertThat(endringer.endringer.harEndringer).isTrue

            verify(exactly = 1) { grunnlagsdataService.hentFraRegister(any()) }
            verify(exactly = 1) { personopplysningerService.finnEndringerIPersonopplysninger(any(), any(), any()) }
            verify(exactly = 1) {
                grunnlagsdataService.oppdaterEndringer(coWithArg {
                    assertThat(it.oppdaterteData).isNotNull
                    assertThat(it.oppdaterteDataHentetTid).isAfter(LocalDateTime.now().minusMinutes(1))
                })
            }
        }
    }

    @Nested
    inner class EndringeneErNyligOppdatert {

        val treTimerSiden = LocalDateTime.now().minusHours(3)

        @Test
        internal fun `skal bruke data fra databasen hvis det gått mindre enn 4h siden siste oppdatering`() {
            mockHentLagretGrunnlagsdata(grunnlagsdata(endringerSjekket = treTimerSiden))

            val endringer = service.hentEndringerPersonopplysninger(behandlingId)

            assertThat(endringer.endringer.harEndringer).isTrue

            verify(exactly = 1) { personopplysningerService.finnEndringerIPersonopplysninger(any(), any(), any()) }
            verify(exactly = 0) { grunnlagsdataService.oppdaterEndringer(any()) }
            verify(exactly = 0) { grunnlagsdataService.hentFraRegister(any()) }
        }

        @Test
        internal fun `skal bruke data fra databasen hvis det gått mindre enn 4h siden siste oppdatering, også når endringer er null`() {
            mockHentLagretGrunnlagsdata(grunnlagsdata(endringerSjekket = treTimerSiden, endringer = null))

            val endringer = service.hentEndringerPersonopplysninger(behandlingId)

            assertThat(endringer.endringer.harEndringer).isFalse

            verify(exactly = 0) { personopplysningerService.finnEndringerIPersonopplysninger(any(), any(), any()) }
            verify(exactly = 0) { grunnlagsdataService.oppdaterEndringer(any()) }
            verify(exactly = 0) { grunnlagsdataService.hentFraRegister(any()) }
        }
    }

    private fun mockHentLagretGrunnlagsdata(grunnlagsdata: Grunnlagsdata) {
        every { grunnlagsdataService.hentLagretGrunnlagsdata(behandlingId) } returns grunnlagsdata
    }

    private fun grunnlagsdata(
        endringerSjekket: LocalDateTime,
        endringer: GrunnlagsdataDomene? = opprettGrunnlagsdata(),
    ) = Grunnlagsdata(
        behandlingId,
        opprettGrunnlagsdata(),
        oppdaterteDataHentetTid = endringerSjekket,
        oppdaterteData = endringer
    )

    private fun mockPersonopplysninger(harEndringer: Boolean) {
        val endringer = Endringer(folkeregisterpersonstatus = Endring(harEndringer = harEndringer))
        every {
            personopplysningerService.finnEndringerIPersonopplysninger(
                any(),
                any(),
                any()
            )
        } returns EndringerIPersonopplysningerDto(LocalDateTime.now(), endringer)
    }
}
