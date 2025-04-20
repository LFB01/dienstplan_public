# Beschreibung des Dienst- und Platzplanungsprogrammes
**mit detaillierter Ablaufbeschreibung** 

---

## Was kann gelöst werden?
- Faire Verplanung von unterschiedlichen Diensten mit Modellierung von:
    - sich überlappenden Diensten
    - Verpflichtenden Dienst-Kombinationen oder Dienst-Verboten
    - Maximal-Anzahl der Dienste
    - Aktiv gewünschte oder nicht-gewünschte Dienste könenn berücksichtigt werden
- Verplanung von Arbeitsplätzen mit Anforderungen wie:
    - Mindest-Kapazitäten
    - "optimaler" Besetzung
    - unter Einhalutng von Rotationen (= zeitlich begrenzte Zuweisung einer- mehrerer Personen zu einem bestimmten Arbeitsplatz)
- Anlegen und Einhalten eines Regelnetzwerkes für die Planung
    - Unter Angabe einer "Wichtigkeit": Muss / Soll / Kann
        - **Regeln zwischen Diensten**
            - *z.B. "Freitag Nachtdienst und Sonntag Tag-Dienst müssen von der selben Person besetzt werden"*
        - **Regeln zwischen Personen**
            - *z.B. "Person 1 und Person 2 dürfen keinen Dienst zusammen machen."*
        - **Regeln zwischen Arbeitsplätzen**
            - Zusätzlich Einbindung einer zeitlichen Komponente:
            - *z.B. "Wer heute den Platz X besetzt, muss morgen den Platz Y besetzen."*
        - **Kombinatiosnregeln**
            - *z.B. "Wer in der Rotation "Sprechstunde" ist, darf keinen Nachtdienst am Montag machen."*
            - oder: *"Wer den Dienst X hat, muss an dem Tag am Platz Y sein."*

---

### Ablauf:
Die Planung kann für Dienste und Plätze separat laufen, für die Platzplanung wird jedoch ein fertiger Dienstplan erwartet. Dieser kann entweder aus dem Progamm oder
(bisher nur manuell) aus einem bestehenden Plan übertragen werden.

**Vorbereitung:**

- Bei Plätzen werden zunächst alle Personen aus einer Rotation **fair** auf die Plätze verteilt. Dabei kann auch eine faire Verteilung gewährleistet werden, wenn die Kapazität der Plätze die Anzahl der Personen einer Rotation übersteigt
- Die mit Personen zu besetzenden Dienste / Plätze werden in eine Priority Queue sortiert:
    - Bei Diensten werden zunächst Wunsch-Dienste und dann die Dienste, für die kein Wunsch bessteht sortiert
        - jeweils revers nach Anzahl der Verfügbaren Personen für diesen Dienst
            - insbesondere Anwesenheit und Dienst-Fähigkeit
    - Bei Plätzen werden zunächst Plätze mit existentert "Optimalbesetzung" (= Liste an Personen, von denen im Idealfall eine diesen Platz besetzen sollte), dann Plätze ohne Idealbesetzung sortiert
        - jeweils wieder revers nach Anzahl der Verfügbaren Personen
            - insbesondere Anwesenheit und Kompetenz

**Planung:**
- Die eigentliche Planung erfolgt über Ziehen aus der Queue. Für jeden Eintrag wird
- Während der Planung wird bereits Rücksicht auf Regeln mit der Gweichtung "MUSS" genommen:
    - Kombinations-Regeln werden direkt gemeinsam verplant:
        - Zum Beispiel eine Person, die beide Dienste einer Dienst-Kombination besetzen kann
    - Verbots-Regeln werden direkt vermieden:
        - Zum Beispiel zwei Personen, die nicht gemeinsam arbeiten können, werden aktiv nicht gemeinsam in (überlappenden/ gemeinsam tätige Dienste / Plätze geplant)
- eine Person wird gewält
- Anpassungen der Queue erfolgt:
    1. Reduktion der verfügbaren Personen nach Entnahme einer Person
    2. Potentielle Konflikte durch geltende Regeln (insbesondere Verbote) in anderen Queue Einträgen


**Regel-Prüfung**
- Nach Planung erfolgt die Prüfung aller Regeln, also auch der bisher nicht bedachten Soll und Kann Regeln.
- Auflistung aller Regeln, die verletzt wurden inklusive "Art" der Verletzung, also ob die Regel nicht eingehalten oder gebrochen wurde

**Regel-Korrektur**
*bisher nur für die Dienstplanung: Dienst-Regeln, Personen-Regeln, Kombinations-Regeln*
- bei gebrochenen Verboten:
    - Suche nach einer neuen Person:
        - entweder aus den verfügbaren, ggf. auch noch nicht verplanten
        - oder Tausch zwischen den bereits eingeplanten Personen, sodass keine wieder zu besetzenden Lücken entstehen
        - oder Suche aus den bereits eingeplanten
- bei fehlenden Kombinationen:
    - Herstellung der Kombiantion z.B. durch Auswahl einer Kombinationsperson für beide Dienste

**Fertiger Plan**
*Ausgabe bisher über die Konsole*
