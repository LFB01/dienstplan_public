package dienstplanung.rules;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dienstplanung.model.DienstForm;
import dienstplanung.model.DienstGruppe;
import dienstplanung.model.Person;
import dienstplanung.logic.DienstPlanung;
import dienstplanung.util.KalenderUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

/**
 * Die Klasse DienstRegel repräsentiert eine Regel,
 * die definiert, wie DienstFormen miteinander verknüpft werden sollen.
 * 
 * Die Klasse erbt von Regel und muss daher zwingend: Gewichtung, Typ und Liste an Ruleable-
 * Objekten an die Elternklasse weiter geben.
 * Ruleable ist ein Interface, das alle regelbaren Klassen implementieren.
 * 
 * Bei Erzeugung einer neuen Regel werden die Ruleable Objekte in das Regelnetzwerk aufgenommen.
 *
 */
//JPA Annotationen zur Datenpersistierung
@Entity
public class DienstRegel extends Regel {

	@ManyToOne
	private DienstForm dienst1;

	@ManyToOne
	private DienstForm dienst2;

    /**
     * Mit der Eltern-Klasse konformer Konsrtuktor zur Erstellung eines DienstRegel Objektes.
	 * Bei Erzeugung eines Objektes werden die Komponenten mit der Regel an das Regelnetzwerk und die Regel der RegelPruefungs-Instanz
	 * übergeben
	 *
     * @param dienst1 - aktuell muss das der chronologisch frühere Dienst sein (bei Regel Mo + Mi also der Mo)
     * @param dienst2
     * @param gewichtung - ob es eine MUSS/ SOLL / KANN Regel ist
     * @param typ - ob es sich um eine Kombination oder ein Verbot handelt
     */
    public DienstRegel(DienstForm dienst1, DienstForm dienst2, Gewichtung gewichtung, RegelTyp typ ) {
    	super(gewichtung, typ, List.of(dienst1, dienst2));
    	this.dienst1 = dienst1;
    	this.dienst2 = dienst2;
    	RegelNetzwerk.objektZuRegelNetzwerkHinzu(dienst1, this);
		RegelNetzwerk.objektZuRegelNetzwerkHinzu(dienst2, this);
		RegelPruefung.getInstance().addDienstRegel(this);
    }

	//Leerer Konstruktor für hibernate
	protected DienstRegel(){
		super();
    }

	/**
	 * Methode zur Initialisierung der Regel, wenn diese über hibernate geladen wird
	 */
	public void initialisiereRegellogik() {
		this.betroffeneObjekte = List.of(dienst1, dienst2);
		RegelNetzwerk.objektZuRegelNetzwerkHinzu(dienst1, this);
		RegelNetzwerk.objektZuRegelNetzwerkHinzu(dienst2, this);
		RegelPruefung.getInstance().addDienstRegel(this);
	}

	// Getter

    /**
     * Getter für die DienstForm 1
     * @return
     */
    public DienstForm getDienst1() {
		return dienst1;
	}


	 /**
     * Getter für die DienstForm 2
     * @return
     */
	public DienstForm getDienst2() {
		return dienst2;
	}


	public DienstForm getAnderenDienst(DienstForm gegeben) {
		if (gegeben.equals(dienst1)) return dienst2;
		else return dienst1;
	}

	public Long getId(){
		return super.id;
	}


	/**
	 * 
	 * Methode zur Überprüfung ob zwei DienstFormen durch die selbe Person besetzt sind, also durch diese 
	 * Person "verknüpft" werden. 
	 * 
	 * Dafür wird geprüft ob der Dienstplan den Dienst1 enthält und die besetzende Person wird geholt.
	 * Dann wird für einen Zeitruam von ** 7 Tagen ** jeder Wochentag, an dem es diesen Dienst gibt, geholt 
	 * und es wird geprüft ob Dienst2 am ersten möglichen Tag durch die selbe Person wie Dienst1 besetzt 
	 * ist 
	 * 
	 * Wichtig: es wird nur der erste Mögliche Tag geprüft!
	 * 
	 * @param heute LocalDate das überprüft wird
	 * @param plan Das Dienstplanungs Objekt 
	 * @return boolean
	 */
	public boolean sindVerknuepft(LocalDate heute, DienstPlanung plan) {
		DienstForm dienst1 = this.dienst1;
		DienstForm dienst2 = this.dienst2;

		Map<DienstForm, Person> diensteHeute = plan.getDiensteAnDatum(heute);
		
		if(diensteHeute == null){
			return false;
		}
		//wenn heute Dienst1, liegt der andere Dienst in der Zukunft
		if (diensteHeute.containsKey(dienst1)) {
			Person personDF1 = diensteHeute.get(dienst1);
			LocalDate kombiTagZukunft = berechneVerbundenenTag(heute, 1);
			if (kombiTagZukunft != null) {
				Map<DienstForm, Person> diensteAmKombiTag = plan.getDiensteAnDatum(kombiTagZukunft);
				if (diensteAmKombiTag!=null) {
					if(diensteAmKombiTag.containsKey(dienst2)) {
						return personDF1.equals(diensteAmKombiTag.get(dienst2));
					}
				}
			}
		}
		//wenn heute Dienst2, liegt der andere Dienst in der Vergangenheit
		if (diensteHeute.containsKey(dienst2)) {
			Person personDF2 = diensteHeute.get(dienst2);
			LocalDate kombiTagVergangenheit = berechneVerbundenenTag(heute, -1);
			if (kombiTagVergangenheit != null) { // Sicherstellen, dass kein Null-Wert verwendet wird
				Map<DienstForm, Person> diensteAmKombiTag = plan.getDiensteAnDatum(kombiTagVergangenheit);
				if (diensteAmKombiTag!=null) {
					if (diensteAmKombiTag.containsKey(dienst1)) {
						return personDF2.equals(diensteAmKombiTag.get(dienst1));
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Methode zur Überprüfung ob eine Regel erfüllt ist, wobei die Methode sindVerknüpft aufgerufen wird.
	 * 
	 * Dabei wird eine existierende Verknüpfung bei KOMBI-Regel **ODER** eine fehlende Verknüpfung bei 
	 * VERBOTS-Regel als OK bewertet. 
	 * 
	 * Fehlt hingegen die Verknüpfung bei einer KOMBI-Regel wird KOMBI_FEHLT zurückgegeben. 
	 * 
	 * Existiert hingegen eine Verknüpfung bei einer VERBOTS-Regel wird VERBOTEN zurückgegeben. 
	 * 
	 * @param heute
	 * @param plan
	 * @return ein RegelStatus Objekt (enum) 
	 */
	public RegelStatus pruefeRegel(LocalDate heute, DienstPlanung plan) {
		boolean sindVerknuepft = sindVerknuepft(heute, plan);
		RegelTyp vorgabe = this.getTyp();
		
		if(sindVerknuepft) {
			return (vorgabe == RegelTyp.KOMBINATION) ? RegelStatus.OK : RegelStatus.VERBOTEN;
		}else {
			return (vorgabe == RegelTyp.KOMBINATION)? RegelStatus.KOMBI_FEHLT : RegelStatus.OK;
		}
	}
	
	/**
	 * Methode, die überprüft, ob am gegebenen Datum eine Regel nötig ist. 
	 * Dafür wird überprüft ob mindestens einer der beiden Dienste aktiv ist.
	 *  
	 * @param datum
	 * @return
	 */
	public boolean istRegelNötig(LocalDate datum) {
		boolean d1;
		boolean d2;
		//Falls es sich um einen Feiertag handelt, zuerst prüfen ob Dienstgruppen auch gültig sind
		if(KalenderUtil.istFeiertag(datum)) {
			if (this.dienst1.getDienstGruppe().isGiltAuchAnFeiertagen() && this.dienst2.getDienstGruppe().isGiltAuchAnFeiertagen()) {
				//Falls Dienstgruppen auch am Feiertag gelten, zurückgeben ob Dienste aktiv sind
				d1 = this.dienst1.ruleableIstAktivAmDatum(datum);
				d2 = this.dienst2.ruleableIstAktivAmDatum(datum);
				return d1||d2;
			}
			return false;
		}
		d1 = this.dienst1.ruleableIstAktivAmDatum(datum);
		d2 = this.dienst2.ruleableIstAktivAmDatum(datum);
		return d1||d2;
	}
	
	/**
	 * Überprüft ob die beiden Dienste in einer Kombination stehen.
	 * 
	 * Die Reihenfolge ist dabei zu beachten!
	 * @param dienstA - muss dem chronologisch früheren Dienst entsprechen
	 * @param dienstB - der chronologisch spätere Dienst
	 * @return boolean ob der Dienst in der Map enthalten ist.
	 */
	public boolean istKombinationErforderlich(DienstForm dienstA, DienstForm dienstB) {
	    return (this.getTyp() == RegelTyp.KOMBINATION) &&
	           (this.dienst1.equals(dienstA) && this.dienst2.equals(dienstB));
	}


    /**
     * Prüft ob die Kombination der beiden eingegebenen Dienste verboten sind. 
     * 
     * Die Reihenfolge ist dabei zu beachten! 
     * @param dienstA - muss dem chronologisch früheren Dienst entsprechen
	 * @param dienstB - der chronologisch spätere Dienst
     * @return boolean ob die Dienst-Kombination verboten ist
     */
	public boolean istVerboten(DienstForm dienstA, DienstForm dienstB) {
	    return (this.getTyp() == RegelTyp.VERBOT) &&
	           (this.dienst1.equals(dienstA) && this.dienst2.equals(dienstB));
	}
    
    
	/**
	 * Methode zur Berechnung eines verbundenen Datums von zwei DienstFormen. Je übergebener Richtung 
	 * erfolgt die Prüfung: 
	 * 
	 * bei 1 : in die Zukunft: 
	 * - Dienst 1 ist der Dienst der Gegenwart und es wird das Datum für Dienst 2 in der Zukunft berechnet
	 * 
	 * bei -1: in die Vergangenheit: 
	 * - Dienst 2 ist der Dienst der Gegenwart und es wird das Datum für Dienst 1 in der Vergangenheit 
	 * 	berechnet
	 * 
	 * JEWEILS wird am Tag "heute" die Suche begonnen, damit auch ein Datum zurückgegeben wird, wenn 
	 * die Dienste am selben Tag stattfinden. 
	 * 
	 * @param heute
	 * @param richtung: 1 für Zukunft und -1 für Vergangenheit
	 * @return
	 */
    public LocalDate berechneVerbundenenTag(LocalDate heute, int richtung) {
    	DienstForm dienst1 = this.dienst1;
    	DienstForm dienst2 = this.dienst2; // Der verbundene Dienst
    	
    	DayOfWeek erlaubteTage1 = dienst1.getBetreffenderTag();
        DayOfWeek erlaubteTage2 = dienst2.getBetreffenderTag();

        //Prüfung in die Zukunft -> also erlaubte Tage von Dienst2
        if (richtung == 1) {
            for (int i = 0; i <= 7; i++) {
                LocalDate potenziellerTag = heute.plusDays(i);
                if (erlaubteTage2.equals(potenziellerTag.getDayOfWeek())) {
                    return potenziellerTag;
                }
            }
        } 
        //Prüfung in die Vergangenheit, also erlaubte Tage von Dienst1
        else if (richtung == -1) {
            for (int i = 0; i <= 7; i++) {
                LocalDate potenziellerTag = heute.minusDays(i);
                if (erlaubteTage1.equals(potenziellerTag.getDayOfWeek())) {
                    return potenziellerTag;
                }
            }
        }
        return null;
    }
    
    
    /**
     * Berechnet den nächsten oder vorherigen gültigen Tag für eine verbundene Dienstform.
     * 
     * @param heute           Das aktuelle Datum des geplanten Dienstes.
     * @param aktuellerDienst Der soeben verplante Dienst.
     * @return Das nächste oder vorherige Datum für die verbundene Dienstform.
     */
    public LocalDate berechneVerbundenenTag(LocalDate heute, DienstForm aktuellerDienst) {
    	
    	int richtung =0;
    	
    	DienstForm dienst1 = this.dienst1;
    	DienstForm dienst2 = this.dienst2; // Der verbundene Dienst
    	
    	if(aktuellerDienst.equals(dienst1)) richtung = 1;
    	if(aktuellerDienst.equals(dienst2)) richtung = -1;

		return berechneVerbundenenTag(heute, richtung);
    }
    
    /**
	 * Ausgabe der Regel als String
	 */
	@Override
	public String toString() {
		return "Die Regel: DienstForm " + this.dienst1 + " steht in einer/ einem " + this.typ + 
				" mit der DienstForm  " + this.dienst2 + " und der "
				+ "Gewichtung: " + this.gewichtung;
	}

}
