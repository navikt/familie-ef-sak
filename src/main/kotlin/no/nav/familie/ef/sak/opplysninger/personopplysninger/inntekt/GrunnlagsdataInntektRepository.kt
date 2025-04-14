package no.nav.familie.ef.sak.opplysninger.personopplysninger.inntekt

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GrunnlagsdataInntektRepository :
    RepositoryInterface<GrunnlagsdataInntekt, UUID>,
    InsertUpdateRepository<GrunnlagsdataInntekt>
