package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
class GrunnlagsdataService(private val grunnlagsdataRepository: GrunnlagsdataRepository,
                           private val søknadService: SøknadService,
                           private val grunnlagsdataRegisterService: GrunnlagsdataRegisterService) {

    fun opprettGrunnlagsdata(behandlingId: UUID) {
        val grunnlagsdata = hentGrunnlagsdataFraRegister(behandlingId)
        grunnlagsdataRepository.insert(Grunnlagsdata(behandlingId = behandlingId, data = grunnlagsdata))
    }

    fun hentGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val grunnlagsdata = hentLagretGrunnlagsdata(behandlingId)
        return GrunnlagsdataMedMetadata(grunnlagsdata.data,
                                        grunnlagsdata.lagtTilEtterFerdigstilling,
                                        grunnlagsdata.sporbar.opprettetTid)
    }

    @Transactional
    fun oppdaterOgHentNyGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        slettGrunnlagsdataHvisFinnes(behandlingId)
        opprettGrunnlagsdata(behandlingId)
        return hentGrunnlagsdata(behandlingId)

    }

    private fun slettGrunnlagsdataHvisFinnes(behandlingId: UUID) {
        grunnlagsdataRepository.deleteById(behandlingId)
    }

    private fun hentLagretGrunnlagsdata(behandlingId: UUID): Grunnlagsdata {
        return grunnlagsdataRepository.findByIdOrThrow(behandlingId)
    }

    private fun hentGrunnlagsdataFraRegister(behandlingId: UUID): GrunnlagsdataDomene {
        val søknad = søknadService.hentOvergangsstønad(behandlingId)
        val personIdent = søknad.fødselsnummer
        val barneforeldreFraSøknad = søknad.barn.mapNotNull { it.annenForelder?.person?.fødselsnummer }
        return hentGrunnlagsdataFraRegister(personIdent, barneforeldreFraSøknad)
    }

    fun hentGrunnlagsdataFraRegister(personIdent: String,
                                     barneforeldreFraSøknad: List<String>): GrunnlagsdataDomene {
        return grunnlagsdataRegisterService.hentGrunnlagsdataFraRegister(personIdent, barneforeldreFraSøknad)
    }
}
