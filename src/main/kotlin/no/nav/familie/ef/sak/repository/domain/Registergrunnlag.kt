package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.MedlemskapRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Registergrunnlag(@Id
                            val behandlingId: UUID,
                            val data: RegistergrunnlagData,
                            val endringer: RegistergrunnlagData? = null,
                            @Version
                            val versjon: Int = 0,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar())

data class RegistergrunnlagData(val medlemskap: MedlemskapRegistergrunnlagDto,
                                val sivilstand: SivilstandRegistergrunnlagDto,
                                val barnMedSamvær: List<BarnMedSamværRegistergrunnlagDto>,
                                val grunnlagsdataType: GrunnlagsdataType = GrunnlagsdataType.BLANKETT_ETTER_FERDIGSTILLING
)

/**
 * Typ som definierer felt som har endringer i [RegistergrunnlagData], eks returnerer den
 * * Hvis det ikke er noen endringer
 *      {medlemskap: [], sivilstand: []}
 *
 * * Hvis det er en endring i medlemskap->statsborgerskap
 *      {medlemskap: [statsborgerskap], sivilstand: []}
 */
typealias Registergrunnlagsendringer = Map<String, List<String>>