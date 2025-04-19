package dienstplanung.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import dienstplanung.model.*;
import dienstplanung.rules.*;
import dienstplanung.util.KalenderUtil;

/**
 * Die Klasse ist für die Planung der Dienste zuständig. Die Planung erfolgt über eine Priority-Queue, die in absteigender Reihenfolge nach
 * Planungsschwierigkeit die Dienste verplant.
 *
 * Es kann auf unterschiedliche Dienstformen mit unterschiedlichen Eigenschfaten (maximale Dienste am Stück / Folgefrei) eingegangen werden
 *
 * Im Planungsvorgang können Regeln mit der höchsten Gewichtung (MUSS) berücksichtig werden.
 *
 *
 */
public class DienstPlanung extends Planung{
	
	private Map<LocalDate, Map<DienstForm, Person>> dienstplan;// Der Dienstplan

	private DienstWuensche dienstWuensche; // enthält eine Person, die sich an einem Datum eine bestimmte Dienstform wünscht
	private List<Person> personen;
	private List<DienstForm> dienstFormen;
	private List<DienstForm> feiertagsDienste;

	//Logging für Debugging
	private static final Logger log = Logger.getLogger(DienstPlanung.class.getName());

	// Record für bessere Lesbarkeit der Queue
	public record DienstDatum(LocalDate datum, DienstForm dienst) {}
	public record PlanungsEintrag(DienstDatum key, Set<Person> kandidaten) {}


	public DienstPlanung(List<Person> personen, List<DienstForm>dienste, 
			DienstWuensche dienstWuensche) {
		this.dienstplan = new HashMap<>();
		this.personen = personen;
		this.dienstFormen =dienste;
		this.dienstWuensche = dienstWuensche;
		this.feiertagsDienste = findeFeiertagsDienste(gruppiereDiensteNachGruppe(dienste));
	}

	//Getter
	public Map<LocalDate, Map<DienstForm, Person>> getDienstplan() {
		return dienstplan;
	}

	public DienstWuensche getDienstWuensche() {
		return dienstWuensche;
	}

	/**
	 * Methode, die für einen Tag alle DienstFormen und eingeteilten Personen zurückgibt
	 * @param datum
	 * @return eine Map aus DienstForm und Personen für den bestimmten Tag
	 */
	public Map<DienstForm, Person> getDiensteAnDatum (LocalDate datum){
		return dienstplan.get(datum);
	}

	/**
	 * Methode zur Bestimmung des Dienstes der Person x am entsprechenden Tag
	 *
	 * Gitb null zurück, sofern kein Dienst an dem Tag vorliegt.
	 * @param datum
	 * @param person
	 * @return
	 */
	public DienstForm getDienstVonPersonAnTag (LocalDate datum, Person person) {
		Map<DienstForm, Person> diensteAmTag = getDiensteAnDatum(datum);
		if (diensteAmTag == null) return null;
		for(Entry<DienstForm, Person> eintrag : diensteAmTag.entrySet()) {
			if(eintrag.getValue().equals(person)) {
				return eintrag.getKey();
			}
		}
		return null;
	}

	// Hilfsmethoden

	/**
	 * Methode zur Gruppierung der Dienstformen nach Gruppen: Es wird eine Map erzeugt, die für jede Gruppe die entsprechenden
	 * Dienstformen enthält:
	 * z.B. Gruppe "Nachtdienst" mit den einzelnen DienstFormen "Nachtdienst" Mo / Di / Mi etc.
	 * @param dienstFormen
	 * @return
	 */
	private Map<DienstGruppe, List<DienstForm>> gruppiereDiensteNachGruppe(List<DienstForm> dienstFormen) {
		return dienstFormen.stream()
				.filter(df -> df.getDienstGruppe() != null)
				.collect(Collectors.groupingBy(DienstForm::getDienstGruppe));
	}

	/**
	 * Methode um alle Dienste, die an einem Feiertag gültig sind und verplant werden müssen, zu finden.
	 * Grundlage ist die Map aus Dienstgruppe und zugehörigen Dienstformen.
	 *
	 * Für jede Gruppe wird geprüft, ob sie an Feiertagen gilt. Wenn, wird der Sonntagsdienst der Gruppe ausgewählt und in
	 * die Feiertags-Dienstform-Liste aufgenommen.
	 * (Warum der Sonntags-Dienst: 1. um Einheitlichkeit zu schaffen und
	 * 2. aufgrund des gleichen Vergütungs-Zuschlags an Sonn- und Feiertagen)
	 *
	 * Achtung, sollte ein Feiertagsdienst existieren, der in gleicher Art NICHT an einem Sonntag stattfindet, ist aktuell keine
	 * Abbildung möglich.
	 *
	 * @param diensteProGruppe
	 * @return
	 */
	private List<DienstForm> findeFeiertagsDienste(Map<DienstGruppe, List<DienstForm>> diensteProGruppe) {
		List<DienstForm> feiertagsDienste = new ArrayList<>();

		for (Map.Entry<DienstGruppe, List<DienstForm>> eintrag : diensteProGruppe.entrySet()) {
			DienstGruppe gruppe = eintrag.getKey();
			if (!gruppe.isGiltAuchAnFeiertagen()) continue;

			List<DienstForm> dienste = eintrag.getValue();

			Optional<DienstForm> sonntagsDienst = dienste.stream()
					.filter(d -> d.getBetreffenderTag() == DayOfWeek.SUNDAY)
					.findFirst();

			sonntagsDienst.ifPresent(feiertagsDienste::add);
		}
		return feiertagsDienste;
	}

	//Planungsmethoden - Vorbereitung
	/**
	 * Methode zur Berechnung der Planungsreihenfolge: Erzeugt wird eine verschachtelte Map, die
	 *  - für jedes Datum
	 *  - jede Dienstform
	 *  	- mit einem Set aus Personen, die diese Dienstform besetzen können UND
	 *  	- einen Boolschen Wert, ob dieser Dienst von einer Person gewünscht wurde
	 *  abbildet.
	 *  Die Map dient als Grundlage für die Planungs-Queue
	 * @param startdatum
	 * @param tage
	 * @return die verschachtelte Map
	 */
	public Map<LocalDate, Map<DienstForm, Map<Boolean, Set<Person>>>> berechnePlanungsReihenfolge(
	        LocalDate startdatum, int tage) {

	    Map<LocalDate, Map<DienstForm, Map<Boolean, Set<Person>>>> reihenfolge = new LinkedHashMap<>();
	    for (int i = 0; i < tage; i++) {
	        LocalDate aktuellerTag = startdatum.plusDays(i);
	        Map<DienstForm, Map<Boolean, Set<Person>>> diensteAnTag = new LinkedHashMap<>();
	        boolean istFeiertag = KalenderUtil.istFeiertag(aktuellerTag);

	        List<DienstForm> relevanteDienste = istFeiertag
	            ? feiertagsDienste.stream()
					.toList()// Feiertagsdienste
	            : dienstFormen.stream()
	                .filter(d -> d.getBetreffenderTag().equals(aktuellerTag.getDayOfWeek()))
	                .toList();  // Normale Dienste

	        for (DienstForm dienst : relevanteDienste) {

				boolean istWunschdienst = dienstWuensche.inWunschListeVorhanden(aktuellerTag, dienst);

	            Set<Person> moeglichePersonen = personen.stream()
	                .filter(p -> personKannDienstBesetzenStatisch(aktuellerTag, p, dienst))
	                .collect(Collectors.toSet());
	            diensteAnTag.put(dienst, Map.of(
	                istWunschdienst, moeglichePersonen
	            ));
	        }
	        reihenfolge.put(aktuellerTag, diensteAnTag);
	    }
	    return reihenfolge;
	}

	
	/**
	 * Methode zur Erstellung einer Planungs Queue auf deren Grundlage, die Verplanung beginnen kann 
	 * Dabei wird die PlanungsMap in eine Planungs Queue umgewandelt.
	 * Der Comparator soritert dabei zuerst:
	 * - WUnschdienste
	 * 		-  sortiert nach Anzahl der Wünsche (also wie viele Personen wünschen sich den Dienst)
	 * 	- dann sowohl normale als auch Wunschdienste nach:
	 * 		- Anzahl der Personen, die den Dienst besetzen können
	 * 		- Diensten, die besonders stark mit Regeln verflochten sind (und demnach schwerer zu planen sind)
	 * @param planungsMap
	 * @return
	 */
	public PriorityQueue<PlanungsEintrag> erstellePlanungsQueue(
			Map<LocalDate, Map<DienstForm, Map<Boolean, Set<Person>>>> planungsMap) {

		PriorityQueue<PlanungsEintrag> planungsQueue =
				new PriorityQueue<>(dienstComparator());

		for (Map.Entry<LocalDate, Map<DienstForm, Map<Boolean, Set<Person>>>> tag : planungsMap.entrySet()) {
			for (Map.Entry<DienstForm, Map<Boolean, Set<Person>>> dienst : tag.getValue().entrySet()) {
				Set<Person> allePersonen = new HashSet<>();
				allePersonen.addAll(dienst.getValue().getOrDefault(true, Set.of()));
				allePersonen.addAll(dienst.getValue().getOrDefault(false, Set.of()));

				planungsQueue.add(new PlanungsEintrag(new DienstDatum(tag.getKey(), dienst.getKey()), allePersonen));
			}
		}
		return planungsQueue;
	}

	// Hilfsmethode - Comparator
	private Comparator<PlanungsEintrag> dienstComparator() {
		return Comparator
				.comparing((PlanungsEintrag e) -> dienstWuensche.inWunschListeVorhanden(e.key().datum(), e.key().dienst()))
				.reversed()
				.thenComparingInt(e -> dienstWuensche.zaehleWuenscheFuerDienstAnTag(e.key().datum(), e.key().dienst()))
				.thenComparingInt(e -> e.kandidaten().size())
				.thenComparingInt(e -> RegelNetzwerk.getFeinprio(e.key().dienst()));
	}

	//Planungsmethoden - Planung
	/**
	 * Plant den nächsten Dienst aus der Queue:
	 * - Prüfung ob direkt eine gemeinsame Verplanung von einer MUSS Dienst-Kombination nötig und möglich ist
	 * - Aufruf der Queue Aktualisierungs-Methode um resultierende Veränderungen zu beachten
	 *
	 * @param planungsQueue Die Prioritätswarteschlange mit offenen Diensten.
	 */
	public void planeNaechstenDienst(PriorityQueue<PlanungsEintrag> planungsQueue) {
	    if (planungsQueue.isEmpty()) {
			 return;
	    }
	    PlanungsEintrag dienstEintrag = planungsQueue.poll();

		LocalDate datum = dienstEintrag.key().datum();
	    DienstForm dienst = dienstEintrag.key().dienst();

		Set<Person> moeglicheKandidaten = dienstEintrag.kandidaten();
	    	
		if (moeglicheKandidaten.isEmpty()) {
			return;
		}
		//Beachtung von Kombinations-MUSS Dienst-Regeln
		Set<DienstRegel> kombiRegeln = findeDienstDienstRegelGewTyp(dienst, Gewichtung.MUSS, RegelTyp.KOMBINATION);

		Person ausgewaehltePerson = null;
		if( !kombiRegeln.isEmpty()) {
			log.info("DIE KOMBIREGELN: " + kombiRegeln);
			boolean kombiErfolgreich = planeKombiDienste(kombiRegeln, dienst, datum, moeglicheKandidaten, planungsQueue);
			//Wenn keine direkte Kombinationsplanung möglich, zunächst Einzelplanung und später Korrektur
			if (!kombiErfolgreich) {
				planeEinzelDienst(dienst, datum, moeglicheKandidaten, planungsQueue);
	    	    }
	    	}

		if( kombiRegeln.isEmpty()) {
			planeEinzelDienst(dienst, datum, moeglicheKandidaten, planungsQueue);
		}
		//Auswirkung der Planung berücksichtigen:
		aktualisierePlanungsQueue(planungsQueue);

	}

	//Hilfsmethoden Planung:

	/**
	 * Plant einen EinzelDienst (ohne Kombinations-Muss-Regel)
	 * Aufrufen der Methode: waehleBestePerson
	 * Die bestePerson besetzt dann den Dienst und es werden die Auswirkungen berrechnet (über Hilfsmethoden):
	 * - Entfernung aus den kommenden Diensten (des selben Tages und ggf. am Folgetag)
	 * - ggf. Entfernung aus mit dem Dienst verbundenen Verbots-Diensten
	 *
	 * @param dienst
	 * @param datum
	 * @param moeglicheKandidaten
	 * @param planungsQueue
	 */
	private void planeEinzelDienst(DienstForm dienst, LocalDate datum,
								   Set<Person >moeglicheKandidaten,
								   PriorityQueue<PlanungsEintrag> planungsQueue) {
		Person ausgewaehltePerson = waehleBestePerson(moeglicheKandidaten, datum, dienst, planungsQueue, false);
		if (ausgewaehltePerson == null) return;
		dienstManuellBesetzen(datum, dienst, ausgewaehltePerson);
		entfernePersonAusZuPlanendenDiensten(planungsQueue, ausgewaehltePerson, datum, dienst);
		entfernePersonAusVerbotsDienstForm(dienst, datum, ausgewaehltePerson, planungsQueue);
	}

	/**
	 * Methode zur Planung von Kombi Diensten
	 * - alle KombiRegeln für den Dienst werden durchlaufen
	 * - alle KombiDienste an allen KombiDaten werden in Listen gespeichert
	 * - damit eine große gemeinsame Menge gebildet werden kann, mit Personen, die an
	 * allen Diensten einsetzbar sind.
	 *
	 * es wird ein boolean zurückgegeben um in der weiteren Planung damit umgehen zu können,
	 * ob die Planung erfolgreich war
	 *
	 * @param kombiRegeln
	 * @param dienst
	 * @param datum
	 * @param moeglicheKandidatenAusgangsdienst
	 * @param planungsQueue
	 */
	private boolean planeKombiDienste(Set<DienstRegel> kombiRegeln, DienstForm dienst, LocalDate datum,
									  Set<Person> moeglicheKandidatenAusgangsdienst,
									  PriorityQueue<PlanungsEintrag> planungsQueue ) {
		Map<DienstForm, LocalDate> kombiDiensteMap = new HashMap<>();
		for (DienstRegel regel : kombiRegeln) {
			DienstForm kombiDienst = regel.getAnderenDienst(dienst);
			LocalDate kombiDatum = regel.berechneVerbundenenTag(datum, dienst);
			kombiDiensteMap.put(kombiDienst, kombiDatum);
		}
		//  Gemeinsame Kandidaten für ALLE Kombi-Dienste ermitteln
		Set<Person> gemeinsameKandidaten = new HashSet<>(moeglicheKandidatenAusgangsdienst);

		for (Map.Entry<DienstForm, LocalDate> entry : kombiDiensteMap.entrySet()) {
			DienstForm kombiDienst = entry.getKey();
			LocalDate kombiDatum = entry.getValue();
			Set<Person> moeglicheKandidatenKombi = getPlanungsKandidaten(kombiDatum, kombiDienst, planungsQueue);
			gemeinsameKandidaten.retainAll(moeglicheKandidatenKombi); // Kandidatenmenge immer weiter reduzieren
		}
		// Falls keine Person alle Bedingungen erfüllt → Abbruch
		if (gemeinsameKandidaten.isEmpty()) {
			return false;
		}
		// Beste Person für ALLE Kombi-Dienste auswählen
		Person ausgewaehltePerson = waehleBestePerson(gemeinsameKandidaten, datum, dienst, planungsQueue, true);

		// Person für Ausgangs- und Kombi-Dienste einplanen
		dienstManuellBesetzen(datum, dienst, ausgewaehltePerson);
		entfernePersonAusZuPlanendenDiensten(planungsQueue, ausgewaehltePerson, datum, dienst);

		for (Map.Entry<DienstForm, LocalDate> entry : kombiDiensteMap.entrySet()) {
			DienstForm kombiDienst = entry.getKey();
			LocalDate kombiDatum = entry.getValue();

			dienstManuellBesetzen(kombiDatum, kombiDienst, ausgewaehltePerson);
			entfernePersonAusZuPlanendenDiensten(planungsQueue, ausgewaehltePerson, kombiDatum, kombiDienst);
		}
		// Person aus verbotenen Diensten entfernen
		entfernePersonAusVerbotsDienstForm(dienst, datum, ausgewaehltePerson, planungsQueue);
		for (Map.Entry<DienstForm, LocalDate> entry : kombiDiensteMap.entrySet()) {
			entfernePersonAusVerbotsDienstForm(entry.getKey(), entry.getValue(), ausgewaehltePerson, planungsQueue);
		}
		// Entferne die geplanten Dienste aus der Queue -->Auslagern als Hilfsmetzhode?
		Iterator<PlanungsEintrag> iterator = planungsQueue.iterator();
		while (iterator.hasNext()) {
			PlanungsEintrag eintrag = iterator.next();
			LocalDate eintragDatum = eintrag.key().datum();
			DienstForm eintragDienst = eintrag.key().dienst();

			// Entferne den Hauptdienst und alle Kombi-Dienste aus der Queue
			if (eintragDatum.equals(datum) && eintragDienst.equals(dienst) || kombiDiensteMap.containsKey(eintragDienst) && kombiDiensteMap.get(eintragDienst).equals(eintragDatum)) {
				iterator.remove();
			}
		}
		return true;
	}

	//Hilfsmethoden Personen-Auswahl

	/**
	 * Wählt die beste verfügbare Person für einen Dienst aus.
	 *  Dabei werden
	 *  1. nur Personen genommen,
	 *  1.1 die noch nicht ihr Maximum an Diensten erreicht haben
	 *  1.2 die in keiner MUSS VERBOTS Kombination mit einer Person stehen, die bereits einen verbundenen Dienst besetzt
	 *
	 *  2. die verbliebenen Personen Sortiert nach
	 *  2.1 ihrer generellen Anzahl an möglichen Einsätzen (je weniger, desto weiter oben in der Liste)
	 *  2.2 ihrer Anzahl an Diensten in diesem Monat
	 *
	 * @param kandidaten Liste der möglichen Kandidaten.
	 * @param datum      Datum des Dienstes.
	 * @param dienst     Der zu verplanende Dienst.
	 * @param ausnahme	Um bei der Planung mehrere Kombidienste Kollisionen mit dientsFrei und DienstFolge zu umgehen, aktuell eher ein Workaround
	 * @return Die ausgewählte Person.
	 */
	public Person waehleBestePerson(Set<Person> kandidaten, LocalDate datum, DienstForm dienst,
									PriorityQueue<PlanungsEintrag> planungsQueue, boolean ausnahme) {
		if (kandidaten.size() == 1) {
			return kandidaten.iterator().next();
		}
		// Wunsch-Personen extrahieren

		Set<Person> wunschPersonen = dienstWuensche.getPersonenFuerDienstAmTag(datum, dienst);

		//Falls Wunsch-Personen vorhanden sind, nur diese weiter betrachten
		if (!wunschPersonen.isEmpty()) {
			Person wunschP =  waehleBesteWunschPerson(wunschPersonen, datum, dienst, planungsQueue, dienstWuensche);
			if(wunschP != null) {
				dienstWuensche.markiereWunschAlsErfuellt(datum, dienst, wunschP);
				return wunschP;
			}
		}
		return kandidaten.stream()
				// Personen filtern, die das Dienstmaximum erreicht haben
				.filter(person -> berechneDiensteDiesenMonat(datum, person) < berechneMaximaleDienste(person.getArbeitspensum())-1)
				.filter(person -> berechneAnzahlDerDienstformDiesenMonat(datum, person, dienst) < dienst.getMaxImMonat())
				// Personen filtern, die durch eine Muss-Verbots-Regel gesperrt sind
				.filter(person -> !istVerbundenerDienstMitVerbotsPersonBesetzt(datum, person, dienst))

				.filter(person-> einplanungInHeutigenDienstBehindertDiensteinteilungMorgenNicht(datum.plusDays(1), person, dienst))
				.filter(person -> ausnahme || !gesternDienstMitFolgeFrei(datum, person))
				.filter (person -> ausnahme ||!gesternDienstFolgeErreicht(datum, person, dienst))
				// Sortieren nach zwei Kriterien:
				//    a) Wer nur an wenigen Tagen arbeiten kann, hat Vorrang
				//    b) Wer bisher am wenigsten eingeplant wurde, hat Vorrang
				.min(Comparator.<Person>comparingDouble(person -> berechneDiensteDiesenMonat(datum, person))
						.thenComparingDouble(person -> berechneMöglicheEinsätzeAusQueue(person, planungsQueue))
						.thenComparingDouble(person -> berechneDiensteDiesenMonat(datum, person) * 0.5)) // Weniger stark priorisieren
				// Falls niemand übrig bleibt, Fehler werfen
				.orElseThrow(() -> new RuntimeException("Kein geeigneter Kandidat für " + dienst + " am " + datum));
	}

	/**
	 * Wählt aus den Personen, die sich den Dienst gewünscht haben, die beste Person aus.
	 * Gibt null zurück, wenn niemand passt.
	 * @param kandidaten
	 * @param datum
	 * @param dienst
	 * @param planungsQueue
	 * @param dienstWuensche
	 * @return
	 */
	private Person waehleBesteWunschPerson(Set<Person> kandidaten, LocalDate datum, DienstForm dienst,
										   PriorityQueue<PlanungsEintrag> planungsQueue, DienstWuensche dienstWuensche) {
		return kandidaten.stream()
				// Personen filtern, die bereits ihr Maximum an Diensten erreicht haben
				.filter(person -> berechneDiensteDiesenMonat(datum, person) < berechneMaximaleDienste(person.getArbeitspensum())-1)
				.filter(person -> berechneAnzahlDerDienstformDiesenMonat(datum, person, dienst) < dienst.getMaxImMonat())

				.min(Comparator.<Person>comparingInt(person -> dienstWuensche.getErfuellteWuensche(person)) // Je mehr erfüllt, desto weiter unten
						.thenComparingInt(person -> -dienstWuensche.getAnzahlAbgegebeneWuensche(person)) // Je weniger abgegeben, desto weiter oben
						.thenComparingInt(person -> berechneMöglicheEinsätzeAusQueue(person, planungsQueue)) // Wer seltener kann, wird bevorzugt
						.thenComparingDouble(person -> berechneDiensteDiesenMonat(datum, person))) // Wer weniger Dienste hat, wird bevorzugt
				// Beste Person wählen oder null zurückgeben
				.orElse(null);
	}

	/**
	 * Methode, die ein Set an Personen zurückgibt, die für einen bestimmten Dienst 
	 * an einem bestimmten Datum geeignet sind.
	 * Grundlage für die übergreifende Suche nach einer Person, die mehrere Dienste besetzen kann (aus einer Kombi)
	 * @param datum
	 * @param dienst
	 * @param planungsQueue
	 * @return
	 */
	private Set<Person> getPlanungsKandidaten(LocalDate datum, DienstForm dienst, 
	        PriorityQueue<PlanungsEintrag> planungsQueue) {
	    for (PlanungsEintrag eintrag : planungsQueue) {
	        LocalDate eintragDatum = eintrag.key().datum();
	        DienstForm eintragDienst = eintrag.key().dienst();

	        if (eintragDatum.equals(datum) && eintragDienst.equals(dienst)) {
	            return new HashSet<>(eintrag.kandidaten()); // Kopie zurückgeben, um Original-Set nicht zu ändern
	        }
	    }
	    return new HashSet<>(); // Falls nichts gefunden wird, leere Menge zurückgeben
	}

	//Methode zur Planungs-Aktualisierung

	/**
	 * Aktualisiert die Prioritätswarteschlange, damit die Reihenfolge nach Veränderungen korrekt bleibt.
	 *
	 * @param planungsQueue Die zu aktualisierende Warteschlange.
	 */
	private void aktualisierePlanungsQueue(
			PriorityQueue<PlanungsEintrag> planungsQueue) {

		List<PlanungsEintrag> alleEintraege = new ArrayList<>(planungsQueue);
		planungsQueue.clear(); // alte Queue leeren
		planungsQueue.addAll(alleEintraege); // erneut einfügen → Sortierung wird neu angewendet
	}


	//Hilfsmethoden - Planungskonsequenzen

	/**
	 * Entfernt eine verplante Person aus allen Diensten des selben Tages und allen zukünftigen Diensten in der Queue,
	 * wenn sie aufgrund eines Folgefrei-Dienstes oder einer In-Folge-Begrenzung nicht mehr arbeiten darf.
	 *
	 * @param planungsQueue Die Prioritätswarteschlange der offenen Dienste.
	 * @param person        Die Person, die nicht mehr verfügbar ist.
	 * @param datum         Das Datum des aktuell geplanten Dienstes.
	 * @param dienst        Die Dienstform des aktuell geplanten Dienstes.
	 */
	private void entfernePersonAusZuPlanendenDiensten(
			PriorityQueue<PlanungsEintrag> planungsQueue,
			Person person, LocalDate datum, DienstForm dienst) {
		// Person aus anderen Diensten am gleichen Tag entfernen
		planungsQueue.forEach(entry -> {
			LocalDate entryDatum = entry.key().datum();
			if (entryDatum.equals(datum)) {
				entry.kandidaten().remove(person); //
			}
		});
		//Dienste am Vortag prüfen (durch die Queue ist eine nicht chronologische Planung möglich) :
		LocalDate gestern = datum.minusDays(1);
		planungsQueue.forEach(entry -> {
			LocalDate entryDatum = entry.key().datum();
			DienstForm vorgesternDienst = entry.key().dienst(); // Der Dienst am Vortag

			if (entryDatum.equals(gestern) && vorgesternDienst.isFolgetagFrei()) {
				entry.kandidaten().remove(person); // Aus allen Diensten entfernen, die durch ein folgefrei die Einplanung in den heutigen Dienst unmöglich machen würden
			}
		});
		//Dienste am Folgetag prüfen
		LocalDate folgetag = datum.plusDays(1);
		boolean hatFolgefrei = dienst.isFolgetagFrei(); // Falls der Dienst ein "Folgefrei" verursacht
		if(hatFolgefrei) {
			planungsQueue.forEach(entry -> {
				LocalDate entryDatum = entry.key.datum();
				if (entryDatum.equals(folgetag)) {
					entry.kandidaten().remove(person); // Aus allen Diensten des Folgetages entfernen
				}
			});
		}
	}

	/***
	 * Methode zur Entfernung einer Person aus der Planungs-Queue, sofern
	 * - für den übergebenen Dienst eine MUSS - VERBOT Regel vorliegt
	 * -> Dann wird für den entsprechende KombiTag die kombinierte Dienst-Form und die ausgewählte Person
	 * aus der Queue genommen, sodass sie nicht verplant werden kann.
	 * @param dienst
	 * @param datum
	 * @param ausgewaehltePerson
	 * @param planungsQueue
	 */
	public void entfernePersonAusVerbotsDienstForm(DienstForm dienst, LocalDate datum, Person ausgewaehltePerson,
												   PriorityQueue<PlanungsEintrag> planungsQueue) {
		if(!findeDienstDienstRegelGewTyp(dienst, Gewichtung.MUSS, RegelTyp.VERBOT).isEmpty()) {
			Set<DienstRegel> dienstVerbotR = findeDienstDienstRegelGewTyp(dienst,Gewichtung.MUSS, RegelTyp.VERBOT);
			for(DienstRegel dr : dienstVerbotR) {
				LocalDate kombiTag = dr.berechneVerbundenenTag(datum, dienst);
				DienstForm d1 =  dr.getDienst1();
				DienstForm d2 = dr.getDienst2();

				if(!d1.equals(dienst)) {
					entfernePersonAusZuPlanendenDiensten(planungsQueue, ausgewaehltePerson, kombiTag, d1);
				}
				if(!d2.equals(dienst)) {
					entfernePersonAusZuPlanendenDiensten(planungsQueue, ausgewaehltePerson, kombiTag, d2);
				}
			}
		}
	}

	//Hilfsmethoden - Regeln

	/**
	 * Filtert alle Dienst-Dienst-Regeln nach Gewichtung und Regeltyp.
	 * 
	 * @param dienst   Die Dienstform, für die Regeln gesucht werden.
	 * @param gewicht  Die Gewichtung der Regel (z. B. MUSS).
	 * @param typ      Der Typ der Regel (z. B. VERBOT oder KOMBI).
	 * @return Ein Set aller passenden Regeln.
	 */
	public Set<DienstRegel> findeDienstDienstRegelGewTyp(DienstForm dienst, Gewichtung gewicht, RegelTyp typ){
		Set<Regel>alleDR= RegelNetzwerk.findeDienstDienstRegeln(dienst);
		Set<Regel>spezRegel = RegelNetzwerk.spezifiziereRegeln(alleDR, gewicht, typ);
		//Nur Regeln vom Typ DienstRegel behalten und sicher casten
	    return spezRegel.stream()
	            .filter(regel -> regel instanceof DienstRegel)
	            .map(regel -> (DienstRegel) regel)
	            .collect(Collectors.toSet());
	}

	/**
	 * Methode zur Überprüfung, ob ein bereits verplanter, verbundener Dienst (wenn Personen miteinander gemeinsam arbeiten) mit einer
	 * Verbots Person besetzt ist.
	 * @param datum
	 * @param person
	 * @param dienst
	 * @return
	 */
	public boolean istVerbundenerDienstMitVerbotsPersonBesetzt(LocalDate datum, Person person, DienstForm dienst) {
		List<DienstForm> verbundeneDienste = dienst.getVerbundeneDienstFormen();
		Map<DienstForm, Person> diensteDesDatum = getDiensteAnDatum(datum);
		if (diensteDesDatum!=null) {
			for(DienstForm vd : verbundeneDienste) {
				Person besetztePerson = diensteDesDatum.getOrDefault(vd, null);
				if (besetztePerson != null && RegelNetzwerk.existiertMUSSVerbotsRegelFuerObjekte(person, besetztePerson)) {
					return true;
				}
			}
		}
		return false;
	}

	//Hilfsmethoden Dienstbesetzung möglich und Berechnungen


	/**
	 * Hilfsmethode zur Überprüfung, ob eine Person einen Dienst besetzen kann:
	 *
	 * Dabei werden statische Faktoren abgeprüft, die bereits vor Beginn der Planung feststehen:
	 *
	 * - Prüfung auf Abwesenheit
	 * - Prüfung auf Dienstfähigkeit
	 * - Prüfung ob gestern ein Dienst mit: Folgefrei ODER MAXIMUM - INFOLGE
	 * - Prüfung ob VerbotsRegel zwischen Person und DienstForm (MUSS)
	 * - Prüfung ob VerbotsRegel zwischen Rotation der Person und DienstFrom
	 * @param datum
	 * @param person
	 * @param dienst
	 * @return
	 */
	public boolean personKannDienstBesetzenStatisch(LocalDate datum, Person person, DienstForm dienst) {
		if (person.istAbwesend(datum) || !person.isDutyFit() ||
				gesternDienstMitFolgeFrei(datum, person) || gesternDienstFolgeErreicht(datum, person, dienst) || dienstWuensche.hatFreiWunsch(datum, person)) {
			return false;
		}
		//Prüfung auf Regelverstoß
		if(RegelNetzwerk.existiertMUSSVerbotsRegelFuerObjekte(person, dienst)) return false;

		if(istPersonEingeplant(datum, person)) {
			DienstForm bereitsBesetzterDienst = getDienstVonPersonAnTag(datum, person);
			if(!RegelNetzwerk.existiertGewichtungTypRegelFuerObjekte(dienst, bereitsBesetzterDienst, Gewichtung.MUSS, RegelTyp.KOMBINATION)) {
				return false;
			}
		}
		//Prüfung ob die Rotation einer Person mit einer VerbotsRegel belegt ist.
		RotationKonkret rotPer = person.getAktiveRotation(datum);
		if (rotPer != null){
			RotationEntwurf entwurf = rotPer.getRotationEntwurf();
			Set<Regel> regelFuerEntwurf = RegelNetzwerk.getRegelnFürObjekt(entwurf);
			for (Regel regelRot : regelFuerEntwurf) {
				if ( regelRot instanceof KombinationsRegel && ((KombinationsRegel) regelRot).hatMUSSVerbot(dienst, entwurf)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Methode, die überprüft, ob eine Person bereits eingeplant ist.
	 * gibt auch false zurück, falls an dem Tag noch keine Dienste geplant wurden
	 * @param datum
	 * @param person
	 * @return boolean ob Person eingeplant ist
	 */
	public boolean istPersonEingeplant(LocalDate datum, Person person) {
		Map<DienstForm, Person> diensteAmTag = dienstplan.get(datum);
		if (diensteAmTag == null) {
			return false;
		}
		// Überprüfen, ob die Person in der Map enthalten ist
		for (Map.Entry<DienstForm, Person> entry : diensteAmTag.entrySet()) {
			if (entry.getValue().equals(person)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Pürft ob eine Einplanung in den heutigen Tag möglich ist.
	 * Dabei wird true zurückgegeben, wenn die person am datum (CAVE da muss in der Regel der Folgetag
	 * eingegeben werden!) eingeteilt ist, der übergebene Dienst aber KEIN Folgefrei hat.
	 *
	 * Ist sie noch nicht eingeteilt wird immer true zurückgegeben.
	 *
	 * @param morgen
	 * @param person
	 * @param dienst
	 * @return
	 */
	public boolean einplanungInHeutigenDienstBehindertDiensteinteilungMorgenNicht(LocalDate morgen, Person person, DienstForm dienst){
		if(istPersonEingeplant(morgen, person)) {
			return !dienst.isFolgetagFrei();
		}
		return true;
	}

	//Hilfsmethode Besetzung

	/**
	 * Methode, um einen Dienst zu besetzen.
	 * @param datum
	 * @param dienst
	 * @param person
	 */
	public void dienstManuellBesetzen(LocalDate datum, DienstForm dienst, Person person ) {
		dienstplan.putIfAbsent(datum, new HashMap<>()); // Initialisiert die Map, falls nötig
		dienstplan.get(datum).put(dienst, person); // Fügt die Kombination hinzu
	}

	/**
	 * Methode zur Entfernung einer Person aus einer DienstForm an einem gegebenen Datum - nötig in der Regelkorrektut
	 * @param datum
	 * @param dienst
	 */
	public void entfernePersonAusDienst(LocalDate datum, DienstForm dienst) {
		if (!dienstplan.containsKey(datum)) {
			return;
		}
		Map<DienstForm, Person> diensteAmTag = dienstplan.get(datum);
		if (!diensteAmTag.containsKey(dienst)) {
			return;
		}
		diensteAmTag.remove(dienst);
		if (diensteAmTag.isEmpty()) {
			dienstplan.remove(datum);
		}
	}
	/**
	 * Überprüft, ob eine Person am Vortag Dienst mit einem Folgefrei hatte.
	 *
	 * @param datum  Das aktuelle Datum.
	 * @param person Die Person, deren Dienst geprüft werden soll.
	 * @return true, wenn die Person am Vortag Dienst hatte, sonst false.
	 *
	 * Die Methode ruft die geplante Person für das Datum des Vortages aus dem `dienst`-Objekt ab
	 * und vergleicht sie mit der übergebenen Person.
	 */
	public boolean gesternDienstMitFolgeFrei (LocalDate datum, Person person) {

		LocalDate gestern = datum.minusDays(1);
		Map<DienstForm, Person>diensteAmTag = getDiensteAnDatum(gestern);

		if (diensteAmTag != null) {
			for (Map.Entry<DienstForm, Person> entry : diensteAmTag.entrySet()) {
				if (entry.getValue().equals(person)) {
					DienstForm dienst = entry.getKey();
					return dienst.isFolgetagFrei();
				}
			}
		}
		return false;
	}

	/**
	 * Prüft ob die Dienstfolge des übergebenen Dienstes  erreicht ist.
	 * @param datum
	 * @param person
	 * @return
	 */
	public boolean gesternDienstFolgeErreicht (LocalDate datum, Person person, DienstForm dienstForm) {
		LocalDate gestern = datum.minusDays(1);
		DienstForm dienstGestern = getDienstVonPersonAnTag(gestern, person);
		if (dienstGestern==null || !dienstGestern.equals(dienstForm)) return false; //Die Person entweder gestern keinen dienst oder hatte zwar gestern dienst, aber nicht den selben, der aktuell verplant wird

		int maxInFolge = dienstForm.getInFolge();
		if (maxInFolge == 1) return true;
		int counter = 0;
		for(int i = 1; i<=maxInFolge; i++){
			LocalDate tag = datum.minusDays(i);
			if(getDienstVonPersonAnTag(tag, person).equals(dienstForm)) counter++;
		}
        return counter == maxInFolge;
    }

	// Hilfsmethode input

	/**
	 * Eine Methode, die den Dienstplan aus einer Vorgabe übernimmt. 
	 * Sollte verwendet werden, wenn die Planung der Dienste außerhalb des Programmes durchgeführt wird.
	 * @param vorgabePlan - Map aus Datum und Map DienstForm und Person
	 */
	public void diensteingabe(Map<LocalDate, Map<DienstForm, Person>> vorgabePlan) {
		for (LocalDate datum : vorgabePlan.keySet()) { // Iteration über alle Daten
			Map<DienstForm, Person> dienste = vorgabePlan.get(datum); // Zu jedem Datum die zugehörige Dienst-Map
			for (DienstForm dienst : dienste.keySet()) { // Iteration über alle Dienstformen an diesem Datum
				Person person = dienste.get(dienst); // Die Person für diese Dienstform holen
				dienstplan.putIfAbsent(datum, new HashMap<>()); // Falls Datum noch nicht in der Map ist, initialisieren
				dienstplan.get(datum).put(dienst, person); // Kombination aus Dienstform und Person speichern
			}
		}
	}

	// Hilfsmethoden Berechnungen
	/**
	 * Methode um dynamisch die Anzahl der Verfügbaren Tage einer Person zu berechnen
	 * @param person
	 * @param planungsQueue
	 * @return
	 */
	private int berechneMöglicheEinsätzeAusQueue(Person person,
												 PriorityQueue<PlanungsEintrag> planungsQueue) {

		return (int) planungsQueue.stream()
				.filter(entry -> entry.kandidaten().contains(person))
				.count();
	}

	 /**
	  * Methode berechnet die maximalen Dienste einer Person. Das MAximum aktuell hart kodiert mit 10
	  * @param arbeitspensum (maximal 1)
	  * @return die gerundete Anzahl der Dienste bei gegebenem Arbeitspensum
	  */
	 public int berechneMaximaleDienste(double arbeitspensum) {
		    int maximaleDiensteVollzeit = 10; //
		    return (int) Math.round(maximaleDiensteVollzeit * arbeitspensum);
		}
	 
	 /**
	  * Berechnet für eine Person die Gesamtanzahl der Dienste unter Berücksichtigung der Gewichtung.
	  * Dabei wird die Gesamtzahl für einen Kalendermonat angegeben.
	  * @param datum
	  * @param person
	  * @return die Gesamtzahl der Dienste als Double
	  */
	 public double berechneDiensteDiesenMonat(LocalDate datum, Person person) {
		 double gesamtgewichtung = 0.0;
		 LocalDate startDesMonats = datum.withDayOfMonth(1);
		 LocalDate endeDesMonats = datum.withDayOfMonth(datum.lengthOfMonth());

		 for (LocalDate tag = startDesMonats; !tag.isAfter(endeDesMonats); tag = tag.plusDays(1)) {
			 Map<DienstForm, Person> diensteAmTag = dienstplan.get(tag);
			 if (diensteAmTag != null) {
				 for (Map.Entry<DienstForm, Person> eintrag : diensteAmTag.entrySet()) {
					 if (eintrag.getValue().equals(person)) {
						 gesamtgewichtung += eintrag.getKey().getGewichtung();
					 }
				 }
			 }
		 }
		 return gesamtgewichtung;
	 }
	 
	 /**
	  * berechnet die Anzahl der DienstForm x, für die die Person bisher im entsprechenden Monat eingetragen ist
	  * @param datum
	  * @param person
	  * @param dienst
	  * @return
	  */
	 public int berechneAnzahlDerDienstformDiesenMonat(LocalDate datum, Person person, DienstForm dienst) {
		    return (int) dienstplan.entrySet().stream()
		        .filter(entry -> entry.getKey().getMonth() == datum.getMonth()) // Nur aktueller Monat
		        .map(Map.Entry::getValue) // Alle geplanten Dienste holen
		        .filter(dienste -> dienste.containsKey(dienst)) // Nur gewünschte Dienstform behalten
		        .map(dienste -> dienste.get(dienst)) // Person auslesen
		        .filter(p -> p.equals(person)) // Prüfen, ob es die richtige Person ist
		        .count();
		}
}
