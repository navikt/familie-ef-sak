# language: no
# encoding: UTF-8

Egenskap: Skolepenger med studiebelastning

  Scenario: En utgift på ett skoleår over grensen, med redusert studiebelastning, skal gi redusert beløp

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 50               | 100_000  |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 34_000 | 1               |

  Scenario: To utgift på ett skoleår under grensen, med redusert studiebelastning

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 50               |              | 30_000   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 50               | 10.2021      | 30_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 30_000 | 1               |
      | 10.2021         | 10.2021         | 4_000  | 1               |

  # Studiebelastning 100%
  # Studiebelastning 50%
  # Disse slås ihop til studiebelastning X # TODO hva er riktig?
  Scenario: To utgift på ett skoleår under grensen, andre utgiften har studiebelastning men får fortsatt full utbetaling

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              |              | 20_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 75               |              | 20_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 75               | 01.2022      | 20_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 20_000 | 2               |
      | 01.2022         | 01.2022         | 20_000 | 2               |

  Scenario: To utgift på ett skoleår under grensen, lavere studiebelastning og har allerede betalt ut maksbeløp

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              |              | 60_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 50               |              | 60_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 50               | 01.2022      | 30_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 60_000 | 2               |

  Scenario: Studiebelastningen øker fra 50 til 100 %, får då ekstra utbetaling for den første perioden (08.2021)

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 50               |              | 60_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              |              | 60_000   |
      | 2            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 10.2021      | 10_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 34_000 | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp  | Kildebehandling |
      | 08.2021         | 08.2021         | 60_000 | 2               |
      | 10.2021         | 10.2021         | 8_000  | 2               |
