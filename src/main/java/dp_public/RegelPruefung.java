package dienstplanung.rules;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import dienstplanung.model.Arbeitsplatz;
import dienstplanung.model.Person;
import dienstplanung.logic.DienstPlanung;
import dienstplanung.logic.PlatzPlanung;

/**
 * Singleton-Klasse zur zentralen Verwaltung und Prüfung aller Regeltypen im Planungssystem.
 *
 * Enthält:
 * - Separierte Speicherung der Regelarten (Dienst, Person, Kombination etc.)
 * - Einheitliche Prüfschnittstellen mit Rückgabe von RegelVerletzungen
 *
 * Alle Regeltypen erben von der gemeinsamen Basisklasse `Regel`.
 * Die Klasse folgt dem Singleton-Prinzip (eine Instanz für das gesamte Programm).
 */

public class RegelPruefung {
	
	private final List<ArbeitsplatzRegel> arbeitsplatzRegeln = new ArrayList<>();
    private final List<PersonenRegel> personenRegeln = new ArrayList<>();
    private final List<DienstRegel> dienstRegeln = new ArrayList<>();
    private final List <KombinationsRegel> kombiRegeln = new ArrayList<>();

    //Singleton- Implementierung
    private static final RegelPruefung INSTANCE = new RegelPruefung();

    private RegelPruefung() {

    }

    public static RegelPruefung getInstance(){
        return INSTANCE;
    }

    /**
     * Folgende Methoden fügen zu prüfende Regeln der jeweiligen Liste hinzu. Die Methode wird im Konstruktor
     * jeder Regel-Subklasse aufgerufen
     */

    public void addArbeitsplatzRegel(ArbeitsplatzRegel regel) {
        arbeitsplatzRegeln.add(regel);
    }

    public void addPersonenRegel(PersonenRegel regel) {
        personenRegeln.add(regel);
    }

    public void addDienstRegel(DienstRegel regel) {
        dienstRegeln.add(regel);
    }
    
    public void addKombiRegel(KombinationsRegel regel) {
    	kombiRegeln.add(regel);
    }


    /**
     * Folgende Methoden entfernen Regeln aus den jeweiligen Liste.
     *
     * Bisher ist nur die Entfernung einer Kombinationsregel im Kontext von Rotationen genutzt
     */

    public void removeArbeitsplatzRegel(ArbeitsplatzRegel regel) {
        if(arbeitsplatzRegeln.contains(regel)) arbeitsplatzRegeln.remove(regel);
    }
    public void removePersonenRegel(PersonenRegel regel) {
        if(personenRegeln.contains(regel)) personenRegeln.remove(regel);
    }

    public void removeDienstRegel(DienstRegel regel) {
    	if(dienstRegeln.contains(regel)) dienstRegeln.remove(regel);
    }

    public void removeKombiRegel(KombinationsRegel regel) {
        if(kombiRegeln.contains(regel)) kombiRegeln.remove(regel);
    }

    //Prüf-Methoden
    
    /**
     * Methode, die die ArbeitsplatzRegeln prüft. 
     * Dabei werden alle Arbeitsplatz Regeln durchlaufen und für jede Regel wird die pruefMethode innerhalb der 
     * ArbeitsplatzRegel Klasse aufgerufen. 
     * 
     * Für jede verletzte Regel wird ein RegelVerletzung Objekt erstellt und in eine Liste gelegt.
     * @param geplanterPlatz
     * @return die Liste der verletzten Regeln
     */
    public List<RegelVerletzung> pruefeArbeitsplatzRegeln(Map<LocalDate, Map<Arbeitsplatz, List<Person>>> geplanterPlatz){
    	List<RegelVerletzung> ergebnisse = new ArrayList<>();
    	for (ArbeitsplatzRegel regel : arbeitsplatzRegeln) {
    		
			for (LocalDate datum : geplanterPlatz.keySet()) {
				if(regel.istRegelNötig(datum)) {
					RegelStatus status = regel.pruefeRegel(datum, geplanterPlatz);
					if(status != RegelStatus.OK) {
						RegelVerletzung pruefErgebnis = new RegelVerletzung(datum, status, regel);
						ergebnisse.add(pruefErgebnis);
					}
				}
			}
    	}
    	return ergebnisse;
    }
    /**
     * Methode, die die DienstRegeln prüft. 
     * Dabei werden alle Dienst Regeln durchlaufen und für jede Regel wird die pruefMethode innerhalb der 
     * PersonenRegel Klasse aufgerufen. 
     * 
     * Für jede verletzte Regel wird ein RegelVerletzung Objekt erstellt und in eine Liste gelegt.
     * @return die Liste der verletzten Regeln
     */
    public List<RegelVerletzung<DienstRegel>> pruefeDienstRegeln(DienstPlanung dienst){
    	List<RegelVerletzung<DienstRegel>> ergebnisse = new ArrayList<>();
    	for (DienstRegel regel : dienstRegeln) {
    		
			for (LocalDate tag : dienst.getDienstplan().keySet()) {
				if(regel.istRegelNötig(tag)) {
					RegelStatus status = regel.pruefeRegel(tag, dienst);
					if(status != RegelStatus.OK) {
						RegelVerletzung<DienstRegel> pruefErgebnis = new RegelVerletzung<>(tag, status, regel);
						ergebnisse.add(pruefErgebnis);
					}
				}
			}
    	}
    	return ergebnisse;
    }
    
    /**
     * Methode, die die PersonenRegeln prüft. 
     * Dabei werden alle Personen Regeln durchlaufen und für jede Regel wird die pruefMethode innerhalb der 
     * PersonenRegel Klasse aufgerufen. 
     * 
     * Für jede verletzte Regel wird ein RegelVerletzung Objekt erstellt und in eine Liste gelegt.
     * @return die Liste der verletzten Regeln
     */
    public List<RegelVerletzung<PersonenRegel>>pruefePersonenRegeln(LocalDate datum, PlatzPlanung plan, DienstPlanung dienst){
    	List<RegelVerletzung<PersonenRegel>> ergebnisse = new ArrayList<>();
    	
        Set<LocalDate> alleTage = new TreeSet<>(); // TreeSet, damit die Tage automatisch sortiert sind
       if (plan != null) alleTage.addAll(plan.getGeplanterPlatz().keySet()); // Alle Tage aus PlatzPlanung
       if( dienst != null) alleTage.addAll(dienst.getDienstplan().keySet()); // Alle Tage aus DienstPlanung
        for (LocalDate tag : alleTage) {
            for (PersonenRegel regel : personenRegeln) {
            	if(regel.istRegelNötig(tag)) {
            		RegelStatus status = regel.pruefeRegel(tag, plan, dienst);
            		if (status != RegelStatus.OK) {
            			ergebnisse.add(new RegelVerletzung<>(tag, status, regel));
            		}
                }
            }
        }
    	return ergebnisse;
    }
    
    /**
     * Methode, die die KombiRegeln prüft. 
     * Dabei werden alle Kombi Regeln durchlaufen und für jede Regel wird die pruefMethode innerhalb der 
     * Kombinations-Regel Klasse aufgerufen.
     * 
     * Für jede verletzte Regel wird ein RegelVerletzung Objekt erstellt und in eine Liste gelegt.
     * @return die Liste der verletzten Regeln
     */
    public List<RegelVerletzung<KombinationsRegel>> pruefeKombinationsRegeln(LocalDate datum, PlatzPlanung plan, DienstPlanung dienst){
    	List<RegelVerletzung<KombinationsRegel>> ergebnisse = new ArrayList<>();
    	
        Set<LocalDate> alleTage = new TreeSet<>(); // TreeSet, damit die Tage automatisch sortiert sind
        if(plan != null)  alleTage.addAll(plan.getGeplanterPlatz().keySet()); // Alle Tage aus PlatzPlanung
        if(dienst != null) alleTage.addAll(dienst.getDienstplan().keySet()); // Alle Tage aus DienstPlanung
        for (LocalDate tag : alleTage) {
            for (KombinationsRegel regel : kombiRegeln) {
            	if(regel.istRegelNötig(tag)) {
            		Ruleable r1 = regel.getObj1();
            		Ruleable r2 = regel.getObj2();
            		RegelStatus status = regel.pruefeRegel(tag, r1, r2, plan, dienst);
            		if (status != RegelStatus.OK) {
            			ergebnisse.add(new RegelVerletzung<>(tag, status, regel));
            		}
                }
            }
        }
    	return ergebnisse;
    }

    @Override 
    public String toString() {
    	return "In den Regeln sind enthalten: Arbeitsplatzregeln: "
    			+ arbeitsplatzRegeln.toString() + " kombiRegeln: " 
    			+ kombiRegeln.toString() + " DienstRgeln: " 
    			+ dienstRegeln.toString() + " und PersonenRegeln: "
    			+ personenRegeln.toString();
    }
}
