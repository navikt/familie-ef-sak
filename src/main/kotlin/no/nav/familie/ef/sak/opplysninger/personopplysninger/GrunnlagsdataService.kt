package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
class GrunnlagsdataService(private val grunnlagsdataRepository: GrunnlagsdataRepository,
                           private val søknadService: SøknadService,
                           private val grunnlagsdataRegisterService: GrunnlagsdataRegisterService,
                           private val behandlingService: BehandlingService,
                           private val fagsakService: FagsakService
) {

    fun opprettGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val grunnlagsdataDomene = hentGrunnlagsdataFraRegister(behandlingId)
        val grunnlagsdata = Grunnlagsdata(behandlingId = behandlingId, data = grunnlagsdataDomene)
        grunnlagsdataRepository.insert(grunnlagsdata)
        return GrunnlagsdataMedMetadata(grunnlagsdata.data,
                                        grunnlagsdata.lagtTilEtterFerdigstilling,
                                        grunnlagsdata.sporbar.opprettetTid)
    }

    fun hentGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val grunnlagsdata = hentLagretGrunnlagsdata(behandlingId)
        return GrunnlagsdataMedMetadata(grunnlagsdata.data,
                                        grunnlagsdata.lagtTilEtterFerdigstilling,
                                        grunnlagsdata.sporbar.opprettetTid)
    }

    @Transactional
    fun oppdaterOgHentNyGrunnlagsdata(behandlingId: UUID): GrunnlagsdataMedMetadata {
        val behandling = behandlingService.hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke laste inn nye grunnlagsdata for behandling med status ${behandling.status}"
        }
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

        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        val søknad = when (stønadstype) {
            StønadType.OVERGANGSSTØNAD -> søknadService.hentOvergangsstønad(behandlingId)
            StønadType.BARNETILSYN -> søknadService.hentBarnetilsyn(behandlingId)
            else -> throw Feil("Ikke implementert støtte for Støndastype $stønadstype")
        }

        return if (søknad == null) {
            hentGrunnlagsdataFraRegister(behandlingService.hentAktivIdent(behandlingId), emptyList())
        } else {
            val personIdent = søknad.fødselsnummer
            val barneforeldreFraSøknad = søknad.barn.mapNotNull { it.annenForelder?.person?.fødselsnummer }
            hentGrunnlagsdataFraRegister(personIdent, barneforeldreFraSøknad)
        }
    }

    fun hentGrunnlagsdataFraRegister(personIdent: String,
                                     barneforeldreFraSøknad: List<String>): GrunnlagsdataDomene {
        return grunnlagsdataRegisterService.hentGrunnlagsdataFraRegister(personIdent, barneforeldreFraSøknad)
    }
}
