# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Sanksjon

  Scenario: Sanksjonerer innvilget periode, innvilger ny periode som sanksjoneres

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Vedtaksperiode |
      | 1            | 01.2021         | 02.2021         |                 |                |
      | 2            | 02.2021         | 02.2021         | SANKSJONERE     | SANKSJON       |
      | 3            | 03.2021         | 04.2021         |                 |                |
      | 4            | 04.2021         | 04.2021         | SANKSJONERE     | SANKSJON       |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Vedtaksperiode |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |                |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 2                     |                |
      | 2            | 02.2021         | 02.2021         |              |                       | SANKSJON       |
      | 3            | 03.2021         | 03.2021         | SPLITTET     | 4                     |                |
      | 3            | 04.2021         | 04.2021         | FJERNET      | 4                     |                |
      | 4            | 04.2021         | 04.2021         |              |                       | SANKSJON       |

