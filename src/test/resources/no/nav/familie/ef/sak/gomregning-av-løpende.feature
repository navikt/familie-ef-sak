# language: no
# encoding: UTF-8

Egenskap: G-omregning

  Scenario: Enkel behandling med 1 periode g-omregnes

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2021         | 12.2021         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt |
      | 1            | 01.2021         | 120000  |

    Og beregner ytelse med G for år 2020 med beløp 101351

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2021         | 12.2021         | 1               | 120000  | 16403 |

    Når utfør g-omregning for år 2021 med beløp 106399

    Så forvent følgende andeler for g-omregnet tilkjent ytelse
      | Fra og med dato | Til og med dato | Inntekt | Beløp |
      | 01.2021         | 04.2021         | 120000  | 16403 |
      | 05.2021         | 12.2021         | 125000  | 17257 |


  Scenariomal: Enkel behandling med 1 periode g-omregnes med 2023 G-beløp

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato |
      | 1            | 01.2023         | 12.2023         |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt   |
      | 1            | 01.2023         | <Inntekt> |

    Og beregner ytelse med G for år 2022 med beløp 111477

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt   | Beløp   |
      | 01.2023         | 12.2023         | 1               | <Inntekt> | <Beløp> |

    Når utfør g-omregning for år 2023 med beløp 118032

    Så forvent følgende andeler for g-omregnet tilkjent ytelse
      | Fra og med dato | Til og med dato | Inntekt                 | Beløp              |
      | 01.2023         | 04.2023         | <Inntekt>               | <Beløp>            |
      | 05.2023         | 12.2023         | <Indeksjustert inntekt> | <G omregnet beløp> |

    Eksempler:
      | Inntekt | Beløp | Indeksjustert inntekt | G omregnet beløp |
      | 10000   | 20902 | 10000                 | 22131            |
      | 120000  | 18492 | 127000                | 19582            |
      | 180000  | 16242 | 190000                | 17219            |
      | 210000  | 15117 | 222000                | 16019            |
      | 240000  | 13992 | 254000                | 14819            |
      | 600000  | 492   | 635000                | 532              |
      | 700000  | 0     | 741000                | 0                |