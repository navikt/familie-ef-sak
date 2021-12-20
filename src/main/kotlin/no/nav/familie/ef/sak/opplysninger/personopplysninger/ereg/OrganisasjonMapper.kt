package no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg

import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon

fun mapEregResultat(organisasjonDto: List<OrganisasjonDto>): List<Organisasjon> = organisasjonDto.map(::mapOrganisasjonDto)

fun mapOrganisasjonDto(organisasjonDto: OrganisasjonDto): Organisasjon = organisasjonDto.let {
    Organisasjon(
            organisasjonsnummer = it.organisasjonsnummer,
            navn = it.navn.redigertnavn ?: it.navn.navnelinje1 ?: it.navn.navnelinje2 ?: "Ukjent navn"
    )
}