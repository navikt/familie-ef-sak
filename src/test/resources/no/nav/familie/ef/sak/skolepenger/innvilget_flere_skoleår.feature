# language: no
# encoding: UTF-8

Egenskap: Skolepenger samme skoleår

  Scenario: et beløp (150_000) som går over 18 måneder splittes opp over 2 skoleår, med utbetaling fra samme dato

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 08.2021      | 100_000  |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2022         | 12.2022         | 100              | 08.2021      | 50_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp   | Kildebehandling |
      | 08.2021         | 08.2021         | 102_000 | 1               |

  Scenario: nytt skoleår med nytt beløp med faktura i nytt skoleår

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 08.2021      | 100_000  |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2022         | 06.2023         | 100              | 08.2022      | 50_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 68_000 | 1               |
      | 08.2022         | 08.2022         | 50_000 | 1               |
