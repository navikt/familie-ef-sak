# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Ulike typer inntekt

  Scenario: Ny periode som følge av ny G

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat |
      | 1            | 01.2023         | 02.2024         | INNVILGE        |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Dagsats | Månedsinntekt |
      | 1            | 01.2023         | 120000  | 100     | 1400          |

    Og G i 2023 er 120_000

    Når beregner ytelse med G

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2023         | 04.2023         | 1               | 162000  | 16917 |
      | 05.2023         | 02.2024         | 1               | 162000  | 18675 |

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Inntekt |
      | 1            | 01.2023         | 04.2023         |              |                       | 162000  |
      | 1            | 05.2023         | 02.2024         |              |                       | 162000  |


  Scenario: Skal bruke totalinntekten for andelen

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat |
      | 1            | 01.2022         | 02.2022         | INNVILGE        |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Dagsats | Månedsinntekt |
      | 1            | 01.2022         | 120000  | 100     | 1400          |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2022         | 02.2022         | 1               | 162000  | 15870 |

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Inntekt |
      | 1            | 01.2022         | 02.2022         |              |                       | 162000  |

  Scenario: hentVedtakForOvergangsstønadFraDato skal returnere riktige inntekter

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat |
      | 1            | 01.2022         | 12.2022         | INNVILGE        |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Dagsats | Månedsinntekt |
      | 1            | 01.2022         | 120000  | 100     | 1400          |
      | 1            | 02.2022         | 90000   | 0       | 0             |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2022         | 01.2022         | 1               | 162000  | 15870 |
      | 02.2022         | 04.2022         | 1               | 90000   | 18570 |
      | 05.2022         | 12.2022         | 1               | 90000   | 19617 |

    Så forvent følgende inntektsperioder fra dato: 01.2022
      | Fra og med dato | Dagsats | Månedsinntekt | Inntekt | Samordningsfradrag |
      | 01.2022         | 100     | 1400          | 120000  | 0                  |
      | 02.2022         | 0       | 0             | 90000   | 0                  |

  Scenario: flere vedtaksperioder med flere inntektsperioder

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat |
      | 1            | 01.2022         | 02.2022         | INNVILGE        |
      | 1            | 03.2022         | 12.2022         | INNVILGE        |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Dagsats | Månedsinntekt |
      | 1            | 01.2022         | 120000  | 100     | 1400          |
      | 1            | 02.2022         | 90000   | 0       | 0             |
      | 1            | 04.2022         | 10000   | 50      | 1000          |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2022         | 01.2022         | 1               | 162000  | 15870 |
      | 02.2022         | 02.2022         | 1               | 90000   | 18570 |
      | 03.2022         | 03.2022         | 1               | 90000   | 18570 |
      | 04.2022         | 04.2022         | 1               | 35000   | 19950 |
      | 05.2022         | 12.2022         | 1               | 35000   | 20902 |

    Så forvent følgende inntektsperioder fra dato: 01.2022
      | Fra og med dato | Dagsats | Månedsinntekt | Inntekt | Samordningsfradrag |
      | 01.2022         | 100     | 1400          | 120000  | 0                  |
      | 02.2022         | 0       | 0             | 90000   | 0                  |
      | 04.2022         | 50      | 1000          | 10000   | 0                  |

