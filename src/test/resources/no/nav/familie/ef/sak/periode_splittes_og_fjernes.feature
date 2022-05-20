# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Splitting og fjerning av periode

  Scenario: Periode splittes og til slutt fjernes

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 04.2021         |
      | 2            | 03.2021         | 04.2021         |
      | 3            | 01.2021         | 04.2021         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 02.2021         |
      | 1            | 03.2021         | 04.2021         |
      | 3            | 01.2021         | 02.2021         |
      | 3            | 03.2021         | 04.2021         |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.2021         | 02.2021         | FJERNET      | 3                     |
      | 3            | 01.2021         | 02.2021         |              |                       |
      | 1            | 03.2021         | 04.2021         | FJERNET      | 2                     |
      | 2            | 03.2021         | 04.2021         | FJERNET      | 3                     |
      | 3            | 03.2021         | 04.2021         |              |                       |
