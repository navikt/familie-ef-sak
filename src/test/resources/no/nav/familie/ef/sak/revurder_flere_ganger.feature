# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Innvilger flere ganger

  Scenario: Innvilger flere ganger med overlappende perioder

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 02.2021         |
      | 2            | 02.2021         | 03.2021         |
      | 3            | 03.2021         | 04.2021         |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |
      | 1            | 02.2021         | 02.2021         | FJERNET      | 2                     |
      | 2            | 02.2021         | 02.2021         | SPLITTET     | 3                     |
      | 2            | 03.2021         | 03.2021         | FJERNET      | 3                     |
      | 3            | 03.2021         | 04.2021         |              |                       |