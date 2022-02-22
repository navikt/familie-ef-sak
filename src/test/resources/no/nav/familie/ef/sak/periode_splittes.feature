# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Vedtak opphører midt i periode

  Scenario: En periode splittes og en periode fjernes som følge av opphør

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.01.2021      | 31.01.2021      |
      | 2            | 01.02.2021      |                 |

    Og følgende andeler tilkjent ytelse
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.01.2021      | 31.03.2021      |
      | 2            | 01.01.2021      | 31.01.2021      |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId |
      | 1            | 01.01.2021      | 31.01.2021      | SPLITTET     | 2                     |
      | 1            | 01.02.2021      | 31.03.2021      | FJERNET      | 2                     |
