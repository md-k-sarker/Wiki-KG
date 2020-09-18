/*
Written by sarker.
Written at 2/13/20.
*/

import analysis.MapWithOtherKG;
import Util.Utility;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class TestMapWithWiki {

    String onto_prefix = "http://www.daselab.com/residue/analysis#";

    public void testsaveInfoToCSV() {
        MapWithOtherKG mapWithOtherKG = new MapWithOtherKG();

        HashMap<String, Collection<String>> sumo_collection_types = new HashMap<>();
        HashSet<String> sumoTypes = new HashSet<>();
        sumoTypes.add("Zaman");
        sumoTypes.add("Kamal");
        sumo_collection_types.put("indi1", sumoTypes);

        HashMap<String, Collection<String>> wiki_collection_types = new HashMap<>();
        HashSet<String> wikiTypes = new HashSet<>();
        wikiTypes.add("Zamanw");
        wikiTypes.add("Kamalw");
        wiki_collection_types.put("indi1", wikiTypes);

        String csvPath = "../data/concepts_mapped_test.csv";
        mapWithOtherKG.saveInfoToCSV(sumo_collection_types, wiki_collection_types, csvPath, "indi", "sumotype", "wikitype");
    }

    public void testSumoSuperClass() {
        try {

            OWLOntology sumoOnto = Utility.loadOntology("");
            IRI iri = IRI.create(onto_prefix + "Graduation");
            OWLClass owlClass = sumoOnto.getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
            System.out.println("owlClass: " + owlClass);
            HashSet<String> sumoParentTypes = EntitySearcher.getSuperClasses(owlClass, sumoOnto).parallelStream().map(type ->
                    Utility.getShortName((OWLClass) type)).collect(Collectors.toCollection(HashSet::new));

            System.out.println("\t parent types: " + sumoParentTypes);

            for (String parent : sumoParentTypes) {
                System.out.println("\t parent types: " + parent);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        TestMapWithWiki testMapWithWiki = new TestMapWithWiki();
        testMapWithWiki.testSumoSuperClass();

    }
}
