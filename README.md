# Dienst- und Platzplanungsprogramm (Java, JavaFX, JPA, Custom Rule Engine)

> **Komplexe Personalplanung im Gesundheitswesen – regelbasiert, fair und flexibel.**


## Projektüberblick

Dieses Projekt ist ein **intelligentes Planungswerkzeug für Dienste und Arbeitsplätze** im stationären medizinischen Kontext.  
Dabei werden u.a. **Dienstwünsche**, **Verfügbarkeiten**, **Feiertage**, **Rotationen** und ein **regelbasiertes System** zur Dienst- 
und Platzvergabe genutzt – alles integriert in eine durchdachte Planungslogik.

**Ziel:** Faire, nachvollziehbare und automatisierte Verteilung von Diensten und Personen unter Berücksichtigung verschiedenster Rahmenbedingungen.

---
## Wer steckt dahinter?

Ich bin ursprünglich Ärztin – und seit 6 Monaten mit Leidenschaft auf dem Weg in die Softwareentwicklung.  
Dieses Projekt ist mein erstes großes Herzensprojekt: **Konzipiert, durchdacht und in Java umgesetzt**, mit vielen Learnings entlang des Weges.

---
## Demo-Video

 [Hier geht’s zum 3-minütigen Demo-Video auf YouTube](https://youtu.be/xRdcgms9Irk)

In diesem Video zeige ich exemplarisch die Bedienung der Oberfläche.

---

### Wichtiger Hinweis zu diesem Repository

Dieses Beispielprojekt zeigt zentrale Komponenten eines komplexen Dienst- und Platzplanungssystems.

Um die Übersichtlichkeit zu wahren, sind in diesem Repo nur ausgewählte Kernelemente enthalten.  
Dazu zählt z. B. die Klasse `DienstPlanung.java`, welche die vollständige Planungslogik abbildet.

#### Warum fehlen Klassen (z. B. `DienstForm`, `Person`, `RegelNetzwerk`)?

Diese Klassen sind stark auf die konkrete Domäne zugeschnitten und dienen vorrangig der Datenstrukturierung.  
Die Planungsklasse ist so kommentiert und entkoppelt, dass ihre Funktionsweise auch ohne diese Klassen nachvollziehbar ist.

---

## Technische Highlights

| Feature                      | Beschreibung                                                                                 |
|------------------------------|----------------------------------------------------------------------------------------------|
| **Prioritätsbasierte Planung** | Wunschdienste & schwer besetzbare Dienste / Plätze werden bevorzugt behandelt                |
| **Regelnetzwerk**            | Dynamisches System für Regeln zwischen Personen, Diensten, Arbeitsplätzen                    |
| **Kombinationslogik**       | Wichtige Kombi-Dienste und -Plätze werden automatisch berücksichtigt                         |
| **Verbotsregeln**          | Inkompatible Kombinationen werden direkt unterbunden                                         |
| **Fairness-Prinzip**       | Einplanung berücksichtigt Wünsche, Frequenz & mögliche Einsatztage                           |
| **Regelprüfung & Korrektur** | Alle Regeln werden im Nachgang überprüft – inklusive stufenweiser minimalinvasiver Korrektur | 

---
## Detaillierte Ablaufbeschreibung

**Mehr Einblick, wie die Planung funktioniert:**  
→ [Hier geht’s zur Programm-Ablaufbeschreibung](docs/programmbeschreibung.md)

---

## Beispielhafte Klassen (im Repository)

### `DienstPlanung.java`
Die zentrale Klasse zur Verplanung der Dienste.  
Beinhaltet: Erstellung der Planungs-Queue, Einhaltung von wichtigen Regeln, Auswahl geeigneter Personen, Feiertagslogik etc.

### `RegelPruefung.java`
Koordiniert die Prüfung sämtlicher Regeln (Personen, Dienste, Plätze).  
Umsetzung via Singleton-Prinzip zur zentralen Verwaltung.

### `DienstRegel.java`
Abbildung einer Regel zwischen zwei Dienstformen (entweder Kombination oder Verbot mit Abstufung der Wichtigkeit).  
Beinhaltet die vollständige Verknüpfungslogik zwischen Diensten.

---

## Tech-Stack 
### Backend 
- **Java:** Logik
- **SQL** und **Hibernate (JPA):** ORM und Datenbankzugriffe
- **SpringBoot:** Services und Repository-Schicht

### Frontend
- **JavaFX** (*bisher noch keine volle UI-Abbildung der Funktionalitäten*)


###  Tools: 
- **Maven** - Build Tool
- **GitHub** (über GitHub Desktop) - Versionskontrolle *(das Repository ist private)*
- **Eclipse, IntelliJ** - Entwicklungsumgebungen

---

## Was habe ich gelernt? 
- Aufbau eines **regelgetriebenen Planungssystems** mit mehreren, teils verzahnten Komponenten
- Anwendung von **OOP- & SOLID-Prinzipien**
- Einsatz von **Hibernate/JPA** und Umgang mit Datenmodellierung
- Umgang mit **Tooling** und strukturiertem Projektaufbau (vom Wasserfall zur inkrementellen Herangehensweise)
- Vor allem aber habe ich gelernt, dass Probleme lösbar sind.

## Was fehlt (noch)?

| Bereich            | Status                             | Anmerkung                          |
|--------------------|------------------------------------|------------------------------------|
| JavaFX-UI          | in Arbeit                          | Viele Features nur per Code sichtbar |
| Platz-Regelprüfung | in Planung                         | Dienste vollständig, Plätze nahezu |
| Regel-Korrektur    | Komplett für Dienste, fehlend für Plätze | nur teilweise automatisiert        |
| Export / Import    | fehlt                              | GUI für Ein-/Ausgabe noch rudimentär |
| Beta-Test          | bald                             | Erste Tests mit Kolleg:innen geplant |

---

**Bereit für Fragen, Feedback oder erste Code Reviews.**  
Ich freue mich über jeden Austausch! 😊
