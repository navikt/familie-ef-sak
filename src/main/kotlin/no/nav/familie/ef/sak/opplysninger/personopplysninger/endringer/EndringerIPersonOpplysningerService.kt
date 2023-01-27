package no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.felles.util.harGåttAntallTimer
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class EndringerIPersonOpplysningerService(
    private val behandlingService: BehandlingService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val personopplysningerService: PersonopplysningerService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentEndringerPersonopplysninger(behandlingId: UUID): EndringerIPersonopplysningerDto {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        feilHvis(behandling.status == BehandlingStatus.FERDIGSTILT) {
            "Kan ikke hente endringer for behandling med status=${behandling.status}"
        }
        val grunnlagsdata = grunnlagsdataService.hentLagretGrunnlagsdata(behandlingId)
        return hentEndringerPersonopplysninger(behandling, grunnlagsdata)
    }

    private fun hentEndringerPersonopplysninger(
        behandling: Saksbehandling,
        grunnlagsdata: Grunnlagsdata,
    ): EndringerIPersonopplysningerDto {
        val skalSjekkeDataFraRegisteret = grunnlagsdata.oppdaterteDataHentetTid.harGåttAntallTimer(4)
        val nyGrunnlagsdata = if (skalSjekkeDataFraRegisteret) {
            grunnlagsdataService.hentFraRegister(behandling.id)
        } else {
            grunnlagsdata.oppdaterteData?.let { GrunnlagsdataMedMetadata(it, grunnlagsdata.oppdaterteDataHentetTid) }
        }
        if (nyGrunnlagsdata == null) {
            return EndringerIPersonopplysningerDto(grunnlagsdata.oppdaterteDataHentetTid, Endringer())
        }

        val endringer = personopplysningerService.finnEndringerIPersonopplysninger(
            behandling,
            grunnlagsdata.tilGrunnlagsdataMedMetadata(),
            nyGrunnlagsdata
        )
        if (skalSjekkeDataFraRegisteret) {
            logger.info(
                "Endringer i fagsak=${behandling.fagsakId} behandling=${behandling.id}" +
                    " ${endringer.endringer.felterMedEndringerString()}"
            )
            grunnlagsdataService.oppdaterEndringer(
                grunnlagsdata.copy(
                    oppdaterteDataHentetTid = LocalDateTime.now(),
                    oppdaterteData = if (endringer.endringer.harEndringer) nyGrunnlagsdata.grunnlagsdata else null
                )
            )
        }
        return endringer
    }
}