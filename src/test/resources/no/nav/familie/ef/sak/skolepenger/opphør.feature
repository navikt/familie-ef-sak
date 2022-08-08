# language: no
# encoding: UTF-8

Egenskap: Skolepenger

  Scenario: Innvilget skolepenger

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Dato faktura | Utgifter | Beløp |
      | 1            | OPPHØRT         | HØGSKOLE_UNIVERSITET | 01.2021         | 03.2021         | 100              | 01.2021      | 200      | 200   |

    Når beregner ytelse kaster feil

    Så forvent følgende feil: Kan ikke opprette tilkjent ytelse for OpphørSkolepenger på førstegangsbehandling

  Scenario: Innvilget skolepenger, opphør det første beløpet

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 08.2021         | 100              | 1         | 08.2021      | 200      | 200   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 09.2021         | 06.2022         | 100              | 2         | 08.2021      | 300      | 300   |
      | 2            | OPPHØRT         | HØGSKOLE_UNIVERSITET | 09.2021         | 06.2022         | 100              | 2         | 08.2021      | 300      | 300   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 300   | 2               |

  Scenario: Innvilget skolepenger, opphør alle perioder

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 08.2021         | 100              | 1         | 08.2021      | 200      | 200   |

    Gitt behandling 2 opphører alle perioder for skolepenger

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |

  Scenario: Innvilget skolepenger, opphør 2 ganger

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Id utgift | Dato faktura | Utgifter | Beløp |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 08.2021         | 100              | 1         | 08.2021      | 200      | 200   |

    Gitt behandling 2 opphører alle perioder for skolepenger

    Gitt behandling 3 opphører alle perioder for skolepenger

    Når beregner ytelse kaster feil

    Så forvent følgende feil: Kan ikke opphøre når det ikke finnes noen perioder å opphøre

