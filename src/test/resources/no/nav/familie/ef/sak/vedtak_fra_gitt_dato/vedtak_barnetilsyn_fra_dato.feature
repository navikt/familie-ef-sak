# language: no
# encoding: UTF-8

Egenskap: hentVedtakForBarnetilsynFraDato

  Scenario: Barnetilsyn behandling med 2 perioder

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Barn | Utgifter | Arbeid aktivitet          |
      | 1            | INNVILGE        | 01.2021         | 03.2021         | Id1  | 200      | ETABLERER_EGEN_VIRKSOMHET |

    Og følgende kontantstøtte
      | BehandlingId | Fra og med dato | Til og med dato | Beløp |
      | 1            | 01.2021         | 03.2021         | 10    |

    Og følgende tilleggsstønad
      | BehandlingId | Fra og med dato | Til og med dato | Beløp |
      | 1            | 01.2021         | 03.2021         | 20    |

    Når beregner ytelse

    # vedtaksperioder
    Så forvent følgende vedtaksperioder fra dato: 11.2020
      | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 01.2021         | 03.2021         | Id1  | 200      |

    Så forvent følgende vedtaksperioder fra dato: 01.2021
      | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 01.2021         | 03.2021         | Id1  | 200      |

    Så forvent følgende vedtaksperioder fra dato: 02.2021
      | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 02.2021         | 03.2021         | Id1  | 200      |

    Så forvent følgende vedtaksperioder fra dato: 03.2021
      | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 03.2021         | 03.2021         | Id1  | 200      |

    Så forvent følgende vedtaksperioder fra dato: 04.2021
      | Fra og med dato | Til og med dato | Barn | Utgifter |

    # kontantstøtte
    Så forvent følgende perioder for kontantstøtte fra dato: 11.2020
      | Fra og med dato | Til og med dato | Beløp |
      | 01.2021         | 03.2021         | 10    |

    Så forvent følgende perioder for kontantstøtte fra dato: 01.2021
      | Fra og med dato | Til og med dato | Beløp |
      | 01.2021         | 03.2021         | 10    |

    Så forvent følgende perioder for kontantstøtte fra dato: 02.2021
      | Fra og med dato | Til og med dato | Beløp |
      | 02.2021         | 03.2021         | 10    |

    Så forvent følgende perioder for kontantstøtte fra dato: 03.2021
      | Fra og med dato | Til og med dato | Beløp |
      | 03.2021         | 03.2021         | 10    |

    Så forvent følgende perioder for kontantstøtte fra dato: 04.2021
      | Fra og med dato | Til og med dato | Beløp |

    # tilleggsstønad
    Så forvent følgende perioder for tilleggsstønad fra dato: 11.2020
      | Fra og med dato | Til og med dato | Beløp |
      | 01.2021         | 03.2021         | 20    |

    Så forvent følgende perioder for tilleggsstønad fra dato: 01.2021
      | Fra og med dato | Til og med dato | Beløp |
      | 01.2021         | 03.2021         | 20    |

    Så forvent følgende perioder for tilleggsstønad fra dato: 02.2021
      | Fra og med dato | Til og med dato | Beløp |
      | 02.2021         | 03.2021         | 20    |

    Så forvent følgende perioder for tilleggsstønad fra dato: 03.2021
      | Fra og med dato | Til og med dato | Beløp |
      | 03.2021         | 03.2021         | 20    |

    Så forvent følgende perioder for tilleggsstønad fra dato: 04.2021
      | Fra og med dato | Til og med dato | Beløp |

  Scenario: Barnetilsyn med oppsplittet historikk
    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 1            | INNVILGE        | 01.2021         | 03.2021         | Id1  | 200      |
      | 2            | INNVILGE        | 02.2021         | 03.2021         | Id1  | 100      |
      | 2            | INNVILGE        | 04.2021         | 04.2021         | Id1  | 50       |

    Når beregner ytelse

    Så forvent følgende vedtaksperioder fra dato: 01.2021
      | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 01.2021         | 01.2021         | Id1  | 200      |
      | 02.2021         | 03.2021         | Id1  | 100      |
      | 04.2021         | 04.2021         | Id1  | 50       |

    Så forvent følgende vedtaksperioder fra dato: 02.2021
      | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 02.2021         | 03.2021         | Id1  | 100      |
      | 04.2021         | 04.2021         | Id1  | 50       |

    Så forvent følgende vedtaksperioder fra dato: 04.2021
      | Fra og med dato | Til og med dato | Barn | Utgifter |
      | 04.2021         | 04.2021         | Id1  | 50       |

  Scenario: Barnetilsyn med ulike barn historikk
    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Fra og med dato | Til og med dato | Barn    | Utgifter |
      | 1            | INNVILGE        | 01.2021         | 03.2021         | Id1     | 200      |
      | 2            | INNVILGE        | 02.2021         | 03.2021         | Id1,Id2 | 100      |
      | 2            | INNVILGE        | 04.2021         | 04.2021         | Id2     | 50       |

    Når beregner ytelse

    Så forvent følgende vedtaksperioder fra dato: 01.2021
      | Fra og med dato | Til og med dato | Barn    | Utgifter |
      | 01.2021         | 01.2021         | Id1     | 200      |
      | 02.2021         | 03.2021         | Id1,Id2 | 100      |
      | 04.2021         | 04.2021         | Id2     | 50       |