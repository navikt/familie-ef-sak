# language: no
# encoding: UTF-8

Egenskap: Utgifter endrer seg for den gitt periode

  Scenario: En ekstra periode med nytt beløp (fra 40 til 40+20)

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 40_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 40_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 20_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 40_000 | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 60_000 | 2               |

  Scenario: Beløp blir lavere, beholder det samme verdiet

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 40_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 20_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 40_000 | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 40_000 | 2               |


  Scenario: Beløp øker, øker utbetalingen

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 20_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 40_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 20_000 | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 40_000 | 2               |
