# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Skal bruke vedtakstidspunkt fra behandling

  Scenario: Bruk vedtakstidspunkt fra behandling

    Gitt følgende behandlinger for overgangsstønad
      | BehandlingId | Behandlingstype       | Vedtaksdato |
      | 1            | FØRSTEGANGSBEHANDLING | 01.01.2021  |
      | 2            | REVURDERING           | 02.02.2021  |

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2022         | 04.2022         |
      | 2            | 03.2022         | 04.2022         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 01.2022         | 0       |
      | 2            | 02.2022         | 0       |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksdato | Endringstype | Endret i behandlingId | Endret i vedtaksdato |
      | 1            | 01.2022         | 02.2022         | 01.01.2021  | SPLITTET     | 2                     | 02.02.2021           |
      | 1            | 03.2022         | 04.2022         | 01.01.2021  | FJERNET      | 2                     | 02.02.2021           |
      | 2            | 03.2022         | 04.2022         | 02.02.2021  |              |                       |                      |
