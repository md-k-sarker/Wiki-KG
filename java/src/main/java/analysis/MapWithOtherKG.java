package analysis;
/*
Written by sarker.
Written at 2/13/20.
*/

import Util.Utility;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class MapWithOtherKG {

    /**
     * Loading wikipedia info from database may be faster.
     */

    String sumoPath = "../data/SUMO.owl";
    String wnPath = "../data/SUMO.owl";
    String wikiPath = "../data/wiki_full_pages_v0_non_cyclic_jan_20_32808131.rdf";

    String csvIndivPath = "../data/indivs_mapped_v0.csv";
    String csvConceptPath = "../data/concepts_mapped.csv";
    String wordNetPath = "";

    private OWLOntology owlOntologySumo;
    private OWLDataFactory owlDataFactorySumo;
    private OWLOntologyManager owlOntologyManagerSumo;

    private OWLOntology owlOntologyWN;
    private OWLDataFactory owlDataFactoryWN;
    private OWLOntologyManager owlOntologyManagerWN;

    private OWLOntology owlOntologyWiki;
    private OWLDataFactory owlDataFactoryWiki;
    private OWLOntologyManager owlOntologyManagerWiki;

    HashSet<String> sumoIndivs;// = new HashSet<String>();
//    HashSet<String> wikiIndivs = new HashSet<String>();

    HashSet<String> sumoConcepts; // = new HashSet<String>();
    //  HashSet<String> wikiConcepts = new HashSet<String>();

    HashMap<String, Collection<String>> sumo_indi_types;// = new HashMap<>();
    HashMap<String, Collection<String>> wiki_indi_types_matched_only; // = new HashMap<>();

    HashMap<String, Collection<String>> sumo_concept_parents_matched_only;// = new HashMap<>();
    HashMap<String, Collection<String>> wiki_concept_parents_matched_only;// = new HashMap<>();

    String onto_prefix = "http://www.daselab.com/residue/analysis#";


    public CSVPrinter openCSVWriter(String filePath) {
        CSVPrinter csvPrinter = null;
        try {
            csvPrinter = new CSVPrinter(new FileWriter(filePath), CSVFormat.DEFAULT.withHeader("Sumo_indiv", "WN_indiv", "Wiki_indiv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvPrinter;
    }

    Collection<String> sumoTypesForIndi = new HashSet<>();
    Collection<String> wikiTypesForIndi = new HashSet<>();

    Collection<String> sumoParentTypes = new HashSet<>();
//    Collection<String> wikiParentTypes = new HashSet<>();

    public void initOntology() {
        try {
            owlOntologySumo = initSumoOntology();
            owlOntologyManagerSumo = owlOntologySumo.getOWLOntologyManager();
            owlDataFactorySumo = owlOntologyManagerSumo.getOWLDataFactory();

            owlOntologyWiki = initWikiOntology();
            owlOntologyManagerWiki = owlOntologyWiki.getOWLOntologyManager();
            owlDataFactoryWiki = owlOntologyManagerWiki.getOWLDataFactory();

            System.out.println("Ontology loaded. ");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void map_concepts() {
        System.out.println("Making concept hashSets.........");
        sumoConcepts = owlOntologySumo.getClassesInSignature().parallelStream().map(concept ->
                Utility.getShortName(concept)).collect(Collectors.toCollection(HashSet::new));
        //wikiConcepts = owlOntologyWiki.getClassesInSignature().parallelStream().map(concept ->
        //      Utility.getShortName(concept)).collect(Collectors.toCollection(HashSet::new));
        System.out.println("Making concept hashSets successfull");
        System.out.println("sumoConcepts size: " + sumoConcepts.size());
//        System.out.println("wikiConcepts size: " + wikiCatsNoCycle.wiki_concept_parents.size());

        System.out.println("Mapping sumo concepts with wiki concepts ................");
        // map super class of sumo class with the superclass of wiki class
        sumo_concept_parents_matched_only = new HashMap<>();
        wiki_concept_parents_matched_only = new HashMap<>();

        IRI iri = IRI.create(onto_prefix + "Graduation");
        OWLClass owlClass = owlOntologySumo.getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
        System.out.println("owlClass: " + owlClass);
        sumoParentTypes = new HashSet<>();
//                wikiParentTypes = new HashSet<>();

        System.out.println("\ttypes....." + EntitySearcher.getSuperClasses(owlClass, owlOntologySumo));

        System.out.println("Mapping sumo concepts with wiki concepts successfull");
        System.out.println("sumo_concept_parents_matched_only size: " + sumo_concept_parents_matched_only.size());
//        System.out.println("wiki_concept_parents_matched_only size: " + wiki_concept_parents_matched_only.size());

        System.out.println("Saving concept csv..............");
//        saveInfoToCSV(sumo_concept_parents_matched_only, wiki_concept_parents_matched_only, csvConceptPath, "concept", "sumoTypes", "wikiTypes");
        System.out.println("Saving concept csv successfull");

    }

    public void map_indivs() {
        try {

            System.out.println("Making indiv hashSets..........");
            sumoIndivs = owlOntologySumo.getIndividualsInSignature().parallelStream().map(indiv ->
                    Utility.getShortName(indiv)).collect(Collectors.toCollection(HashSet::new));
            //wikiIndivs = owlOntologyWiki.getIndividualsInSignature().parallelStream().map(indiv ->
            //      Utility.getShortName(indiv)).collect(Collectors.toCollection(HashSet::new));
            System.out.println("Making indiv hashSets successfull");
            System.out.println("sumoIndivs size: " + sumoIndivs.size());
            System.out.println("wikiIndivs size: " + wikiCatsNoCycleWithPages.wiki_indi_types.size());

            System.out.println("Mapping sumo indi with wiki indi................");
            // map type of sumo indi with the type of wiki indi
            sumo_indi_types = new HashMap<>();
            wiki_indi_types_matched_only = new HashMap<>();

            sumoIndivs.forEach(sumoIndi -> {
                if (wikiCatsNoCycleWithPages.wiki_indi_types.containsKey(sumoIndi)) {
                    IRI iri = IRI.create(onto_prefix + sumoIndi);
                    OWLNamedIndividual owlNamedIndividual = owlDataFactorySumo.getOWLNamedIndividual(iri);

                    sumoTypesForIndi = new HashSet<>();
                    wikiTypesForIndi = new HashSet<>();

                    // search type in sumo
                    sumoTypesForIndi = EntitySearcher.getTypes(owlNamedIndividual, owlOntologySumo).parallelStream().map(type ->
                            Utility.getShortName((OWLClass) type)).collect(Collectors.toCollection(HashSet::new));
                    sumo_indi_types.put(sumoIndi, sumoTypesForIndi);

                    wiki_indi_types_matched_only.put(sumoIndi, wikiCatsNoCycleWithPages.wiki_indi_types.get(sumoIndi));
                }
            });
            System.out.println("Mapping sumo indi with wiki indi successfull");
            System.out.println("wiki_indi_types size: " + wiki_indi_types_matched_only.size());

            System.out.println("Saving indi csv..............");
            saveInfoToCSV(sumo_indi_types, wiki_indi_types_matched_only, csvIndivPath, "indiv", "sumoType", "wikiType");
            System.out.println("Saving indi csv successfull");


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void map_entity() {
//        map_concepts();
        map_indivs();
    }


    public void saveInfoToCSV(HashMap<String, Collection<String>> sumo_collection_types, HashMap<String,
            Collection<String>> wiki_collection_types, String csvPath, String... header) {
        try {

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(csvPath));
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.DEFAULT.withHeader(header));

            // write indi types or class types
            for (Map.Entry<String, Collection<String>> entry : sumo_collection_types.entrySet()) {
                String indi_name = entry.getKey();
                StringBuilder sumoTypes = new StringBuilder();

                for (String eachSumoType : entry.getValue()) {
                    sumoTypes.append(eachSumoType + ";");
                }
                String sumoTypesStr = sumoTypes.toString();
//                sumoTypesStr = sumoTypesStr.substring(0, sumoTypesStr.length() - 2);

                StringBuilder wikiTypes = new StringBuilder();
                for (String eachWikiType : wiki_collection_types.get(indi_name)) {
                    wikiTypes.append(eachWikiType + ";");
                }


                String wikiTypesStr = wikiTypes.toString();
//                wikiTypesStr = wikiTypesStr.substring(0, wikiTypesStr.length() - 2);

                csvPrinter.printRecord(indi_name, sumoTypesStr, wikiTypesStr);
            }

            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public OWLOntology initSumoOntology() throws OWLOntologyCreationException, IOException {
        owlOntologySumo = Utility.loadOntology(sumoPath);
        return owlOntologySumo;
    }

    WikiCatsNoCycleWithPages wikiCatsNoCycleWithPages;

    public OWLOntology initWikiOntology() throws Exception {
        wikiCatsNoCycleWithPages = new WikiCatsNoCycleWithPages();
        wikiCatsNoCycleWithPages.initData();

        final long readDatabaseStartTime = System.currentTimeMillis();
        wikiCatsNoCycleWithPages.cachePageTitles();
        wikiCatsNoCycleWithPages.traverseDataBaseBFS();
        final long readDatabaseEndTime = System.currentTimeMillis();
        System.out.println("Databse read+traverse time: " + (readDatabaseEndTime - readDatabaseStartTime) / 60000 + " minutes");

        wikiCatsNoCycleWithPages.closeConnections();
        wikiCatsNoCycleWithPages.freeMemory();
        return wikiCatsNoCycleWithPages.owlOntology;
    }


    public void correctSumoConceptTypes() {
        try {
            OWLOntology sumoOnto = Utility.loadOntology("../data/SUMO_properly_named.owl");

            CSVPrinter csvPrinter = new CSVPrinter(new BufferedWriter(new
                    FileWriter("../data/concepts_mapped_v1.csv")),
                    CSVFormat.DEFAULT.withHeader("concept", "sumoTypes", "wikiTypes"));

            CSVParser csvParser = Utility.parseCSV("../data/concepts_mapped_v0.csv", true);
            csvParser.forEach(strings -> {
                String conceptName = strings.get("concept");
                String wikiTypesStr = strings.get("wikiTypes");
                System.out.println("conceptname: " + conceptName);

                IRI iri = IRI.create(onto_prefix + conceptName);
                OWLClass owlClass = sumoOnto.getOWLOntologyManager().getOWLDataFactory().getOWLClass(iri);
                System.out.println("\towlclass: " + owlClass);

                HashSet<String> sumoParentTypes = EntitySearcher.getSuperClasses(owlClass, sumoOnto).parallelStream().map(type ->
                        Utility.getShortName((OWLClass) type)).collect(Collectors.toCollection(HashSet::new));
                System.out.println("\t parent types: " + sumoParentTypes);

                StringBuilder sumoTypes = new StringBuilder();
                for (String eachSumoType : sumoParentTypes) {
                    sumoTypes.append(eachSumoType + ";");
                }
                String sumoTypesStr = sumoTypes.toString();

                try {
                    csvPrinter.printRecord(conceptName, sumoTypesStr, wikiTypesStr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            csvPrinter.flush();
            csvPrinter.close();

            csvParser.close();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void correctSumoIndivTypes() {
        try {
            OWLOntology sumoOnto = Utility.loadOntology("../data/SUMO_properly_named.owl");

            CSVPrinter csvPrinter = new CSVPrinter(new BufferedWriter(new
                    FileWriter("../data/indivs_mapped_v1.csv")),
                    CSVFormat.DEFAULT.withHeader("concept", "sumoTypes", "wikiTypes"));

            CSVParser csvParser = Utility.parseCSV("../data/indivs_mapped_v0.csv", true);
            csvParser.forEach(strings -> {
                String indivName = strings.get("indiv");
                String wikiTypesStr = strings.get("wikiTypes");
                System.out.println("indivName: " + indivName);

                IRI iri = IRI.create(onto_prefix + indivName);
                OWLNamedIndividual owlNamedIndividual = sumoOnto.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(iri);
                System.out.println("\towlNamedIndividual: " + owlNamedIndividual);

                HashSet<String> sumoParentTypes = EntitySearcher.getTypes(owlNamedIndividual, sumoOnto).parallelStream().map(type ->
                        Utility.getShortName((OWLClass) type)).collect(Collectors.toCollection(HashSet::new));
                System.out.println("\t parent types: " + sumoParentTypes);

                StringBuilder sumoTypes = new StringBuilder();
                for (String eachSumoType : sumoParentTypes) {
                    sumoTypes.append(eachSumoType + ";");
                }
                String sumoTypesStr = sumoTypes.toString();

                try {
                    csvPrinter.printRecord(indivName, sumoTypesStr, wikiTypesStr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            csvPrinter.flush();
            csvPrinter.close();

            csvParser.close();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Finding the matching concepts on sumo.
     * We already have the concepts (which are same in wiki and dbpedia),
     * and then we are just finding the concepts which also exist in sumo.
     */
    public void find_matching_concepts_on_sumo() {

        String[] concepts_db_wiki = new String[]{"Aircraft", "Anime", "Archaea", "Area", "Bacteria", "Beer", "Birth", "Boxing", "Brain", "Building", "Celebrity", "Cheese", "City", "Community", "Competition", "Currency", "Death", "Demographics", "Divorce", "Drama", "Embryology", "Family", "Fashion", "Film", "Fish", "Grape", "Infrastructure", "Language", "Law", "Manga", "Manhua", "Manhwa", "Marriage", "Medicine", "Opera", "Painting", "Population", "Rebellion", "Reference", "Royalty", "Sales", "Sculpture", "Software", "Sound", "Spacecraft", "Species", "Surname", "Tax", "Theatre", "Town", "Wine", "Work"};

        String sumo_onto_path = "../data/KGS/SUMO_properly_named.owl";

        try {
            System.out.println("loading ontology");
            // load sumo
            OWLOntology sumoOnto = Utility.loadOntology(sumo_onto_path);
            System.out.println("loading ontology finished");

            // do reasoning on sumo
            System.out.println("Reasoning ontology");
            OWLReasoner owlReasoner = Utility.initReasoner("elk", sumoOnto, null);
            System.out.println("Reasoning ontology finished");

            // cache sumo concepts and parents
            System.out.println("Caching sumo ontology");
            HashMap<String, HashSet<String>> sumo_concepts_and_parents = new HashMap<>();
            sumoOnto.getClassesInSignature().parallelStream().forEach(owlClass -> {

                HashSet<String> parent_classes = new HashSet<>();

                owlReasoner.getSuperClasses(owlClass, true).getFlattened().forEach(parent_class -> {
                    if (parent_class instanceof OWLClass) {
                        parent_classes.add(Utility.getShortName(parent_class));
                    } else {
                        System.out.println("Parent class: " + parent_class + " not instance of owlClass");
                    }
                });
                sumo_concepts_and_parents.put(Utility.getShortName(owlClass), parent_classes);
            });
            System.out.println("Caching sumo ontology finished");

            // iterate ove the 52 concepts
            System.out.println("Finding same concepts on sumo ontology");
            for (int i = 0; i < concepts_db_wiki.length; i++) {

                if (sumo_concepts_and_parents.keySet().contains(concepts_db_wiki[i])) {

                    // need to get the parents of this class on sumo
                    System.out.println("concept: " + concepts_db_wiki[i] + ", parents: "
                            + sumo_concepts_and_parents.get(concepts_db_wiki[i]));
                }
            }
            System.out.println("Finding same concepts on sumo ontology finished");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void main(String[] args) {
        MapWithOtherKG mapWithOtherKG = new MapWithOtherKG();
        mapWithOtherKG.find_matching_concepts_on_sumo();
//        mapWithOtherKG.initOntology();
//        mapWithOtherKG.map_entity();
    }
}
