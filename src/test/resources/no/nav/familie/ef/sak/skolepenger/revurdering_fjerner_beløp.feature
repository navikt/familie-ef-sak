# language: no
# encoding: UTF-8

Egenskap: Revurdering fjerner beløp

  Scenario: Fjerner beløp helt, og legger inn nytt beløp som utbetales fra senere dato

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              |              | 40_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 10.2021      | 20_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 40_000 | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 10.2021         | 10.2021         | 20_000 | 2               |
