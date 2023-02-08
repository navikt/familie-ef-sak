package no.nav.familie.ef.sak.opplysninger.mapper

import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper

fun barnMedSamværMapper() = BarnMedSamværMapper(AdresseMapper(KodeverkServiceMock().kodeverkService()))