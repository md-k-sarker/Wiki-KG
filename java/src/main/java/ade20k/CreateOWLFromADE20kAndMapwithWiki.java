package ade20k;

import Util.Utility;
import Util.ConfigParams;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Create Ontology from the conf file.
 */
public class CreateOWLFromADE20kAndMapwithWiki {

    // public static String Util.ConfigParams.namespace ="http://www.daselab.com/residue/analysis#"
    public static String rootADE20KPath = "../data/ade20k_images_and_owls";
    public static String imageContainsObjPropertyName = "imageContains";

    // wiki cats ontology or wikip pages ontology or a combined one
    public static String wikiOntoPath = "../data/KGS/automated_wiki/wiki_cats_v1_non_cyclic.owl";
    public static HashSet<String> wikiIndivsHashSet = new HashSet<>();
    public static HashSet<String> wikiConceptsHashSet = new HashSet<>();
    public static OWLOntology owlOntologyWiki;

    /**
     * The objects which has same (string similarity) name as of any wiki pages are saved to this variable.
     * For example, objects names are in column[3]/wordnet and in column[4]/normal_names column.
     * Those names are matched with wikipedia pages ontology.
     * If same name found then it is stored in this variable.
     */
    public static HashSet<String> matchedObjectswithWikiPages = new HashSet<>();
    /**
     * The objects which has same (string similarity) name as of any wiki concepts are saved to this variable.
     * For example, objects names are in column[3]/wordnet and in column[4]/normal_names column.
     * Those names are matched with wikipedia pages ontology.
     * If same name found then it is stored in this variable.
     */
    public static HashSet<String> matchedObjectswithWikiConcepts = new HashSet<>();
    public static String csvPathForMatchedEntity = "../data/matched_objects_with_wiki_concepts.csv";
    public static String csvPathForAllEntity = "../data/all_objects_in_ADE20K.csv";
    public static String csvPathForNonMatchedEntity = "../data/non_matched_objects_with_wiki_concepts.csv";
    /**
     * All the objects in ade20 images.
     */
    public static HashSet<String> allObjectsInADE20K = new HashSet<>();


    public static int counter = 0;

    public static void main(String[] args) {
        try {

            // fixing the base iri:
            ConfigParams.namespace = "http://www.daselab.com/residue/analysis";

            // load wikipedia page ontology and cache the individuals
            System.out.println("Loading the ontology.......");
            System.out.println("Onto path: " + wikiOntoPath);
            Long startTime = System.currentTimeMillis();
            owlOntologyWiki = Utility.loadOntology(wikiOntoPath);
            wikiConceptsHashSet = owlOntologyWiki.getClassesInSignature().stream()
                    .map(owlClass -> Utility.getShortName(owlClass))
                    .collect(Collectors.toCollection(HashSet::new));
            System.out.println("Loading the ontology successfull");
            Long loadEndTime = System.currentTimeMillis();
            System.out.println("Ontology load time: " + (loadEndTime - startTime) / 1000 + " seconds");


            System.out.println("Traversing and creating the ontology............");
            // traverse the text files
            Files.walk(Paths.get(rootADE20KPath)).
                    filter(f -> f.toFile().isFile() && f.toFile().getAbsolutePath().endsWith(".txt")).
                    forEach(f -> {
                        try {
//                            createSuperClassName(f);
                            createOWL(f);
                            printStatus(f.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Error Occurred: " + e.toString());
                        }
                    });

            System.out.println("Traversing and creating the ontology successfull");
            Long createEndTime = System.currentTimeMillis();
            System.out.println("Traversing and creating time: " + (createEndTime - loadEndTime) / 1000 + " seconds");

            // print total entity size
            System.out.println("Total objects in ADE20K: " + allObjectsInADE20K.size());
            System.out.println("Matched objects in ADE20K with Wiki: " + matchedObjectswithWikiConcepts.size());

            System.out.println("Saving to csv.............");
            // save all entities to csv file
            ArrayList<String> columnNamesForAllEntity = new ArrayList<>();
            columnNamesForAllEntity.add("AllEntities");
//            if (Utility.writeToCSV(csvPathForAllEntity, columnNamesForAllEntity, new ArrayList<>(allObjectsInADE20K))) {
//                System.out.println("Wrote csv successfully at: " + csvPathForAllEntity);
//            } else {
//                System.out.println("Failed to write csv at: " + csvPathForAllEntity);
//            }

            // save the matched entities to csv file
            ArrayList<String> columnNamesForMatchedEntity = new ArrayList<>();
            columnNamesForMatchedEntity.add("MatchedEntities");
            if (Utility.writeToCSV(csvPathForMatchedEntity, columnNamesForMatchedEntity, new ArrayList<>(matchedObjectswithWikiConcepts))) {
                System.out.println("Wrote csv successfully at: " + csvPathForMatchedEntity);
            } else {
                System.out.println("Failed to write csv at: " + csvPathForMatchedEntity);
            }


            // save the non-matched entities to csv file
            ArrayList<String> columnNamesForNonMatchedEntity = new ArrayList<>();
            columnNamesForNonMatchedEntity.add("NonMatchedEntities");
            allObjectsInADE20K.removeAll(matchedObjectswithWikiConcepts);
            if (Utility.writeToCSV(csvPathForNonMatchedEntity, columnNamesForNonMatchedEntity, new ArrayList<>(allObjectsInADE20K))) {
                System.out.println("Wrote csv successfully at: " + csvPathForNonMatchedEntity);
            } else {
                System.out.println("Failed to write csv at: " + csvPathForNonMatchedEntity);
            }

            System.out.println("Saving to csv successfull");
            Long csvEndTime = System.currentTimeMillis();
            System.out.println("Saving to csv time: " + (csvEndTime - createEndTime) / 1000 + " seconds");


            System.out.println("Total time: " + (csvEndTime - startTime) / 1000 + " seconds");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void processSuperClass(Path path) {
        String parent = path.getParent().getFileName().toString();
        // added on may 13
        String[] parts = parent.split("_");
        parent = "";
        for (int i = 0; i < parts.length; i++) {
            parent = parent + parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1).toLowerCase();
        }
        //parent = parent.substring(0, 1).toUpperCase() + parent.substring(1).toLowerCase();

        String grandParent = path.getParent().getParent().getFileName().toString();
        String[] gParts = grandParent.split("_");
        grandParent = "";
        for (int i = 0; i < gParts.length; i++) {
            grandParent = grandParent + gParts[i].substring(0, 1).toUpperCase() + gParts[i].substring(1).toLowerCase();
        }
        //grandParent = grandParent.substring(0, 1).toUpperCase() + grandParent.substring(1).toLowerCase();

        String owl_class_name = "";
        String owl_super_class_name = "";
        boolean shouldCreateSuperClass = false;

        // Condition
        // If parent name is misc, then parent folder name is misc
        // If grandparent is not a....z or outliers then class name should be
        // parent_name and grand_parent_name
        if (parent.equals("misc")) {
            owl_class_name = "misc";
        } else if ((grandParent.length() == 1 || grandParent.equals("outliers"))) {
            owl_class_name = parent;
        } else if ((parent.equalsIgnoreCase("Outdoor") || parent.equalsIgnoreCase("Indoor"))) {
            owl_class_name = grandParent;
        } else {
            owl_class_name = parent;
            owl_super_class_name = grandParent;
            shouldCreateSuperClass = true;
            System.out.println("else condition: " + "\n\towl_class_name: " + owl_class_name + "\n\towl_super_class_name: " + owl_super_class_name);
        }

        // call to create owl files
        try {
            printStatus(path.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void printStatus(String status) {
        try {
            counter++;
            System.out.println("creating owl from file: " + status + " is successfull");
            System.out.println("Processed " + counter + " files");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param filePath
     * @throws Exception
     */
    public static void createOWL(Path filePath) throws Exception {


        File f = filePath.toFile();
        String imageName = f.getName().replaceAll("_atr.txt", "");
        // System.out.println("debug: imageName: " + imageName);

        // Create Ontology
        OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();
        OWLDataFactory owlDataFactory = owlManager.getOWLDataFactory();

        IRI ontologyIRI = IRI.create(ConfigParams.namespace);

        String temp = f.getAbsolutePath().replaceAll("_atr.txt", "_mapped_with_wiki_concept.owl");
        String diskFileName = temp.replace("\\", "/");
        IRI owlDiskFileIRI = IRI.create("file:" + diskFileName);


        OWLOntology ontology = owlManager.createOntology(ontologyIRI);
        System.out.println("created ontology: " + ontology.getOntologyID());

        // create individual
        IRI iriIndi = IRI.create(ConfigParams.namespace + "#" + imageName);
//        System.out.println("iriIndi: " + iriIndi);
        OWLNamedIndividual namedIndiImage = owlDataFactory.getOWLNamedIndividual(iriIndi);
        OWLAxiom indivDeclrAxiom = owlDataFactory.getOWLDeclarationAxiom(namedIndiImage);
        AddAxiom indivAddAxiom = new AddAxiom(ontology, indivDeclrAxiom);
        owlManager.applyChange(indivAddAxiom);


        // create object property
        IRI iriObjectProp = IRI.create(ConfigParams.namespace + "#" + imageContainsObjPropertyName);
        OWLObjectProperty owlObjPropImageContains = owlDataFactory.getOWLObjectProperty(iriObjectProp);
        OWLAxiom objPropAxiom = owlDataFactory.getOWLDeclarationAxiom(owlObjPropImageContains);
        AddAxiom objPropAddAxiom = new AddAxiom(ontology, objPropAxiom);
        owlManager.applyChange(objPropAddAxiom);


        // assign individual to class
        // do not assign it the corresponding class, instead assign it to OWL:Thing
//        System.out.println("Individual Name: " + namedIndiImage.getIRI().toString());
        OWLClassAssertionAxiom owlClassAssertionAxiom = owlDataFactory
                .getOWLClassAssertionAxiom(owlDataFactory.getOWLThing(), namedIndiImage);
        AddAxiom addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
        owlManager.applyChange(addAxiom);

        // Read files and Parse data
        FileReader reader = new FileReader(filePath.toString());
        BufferedReader bfr = new BufferedReader(reader);

        String line;
        // Set<String> terms = new HashSet<>();

        while ((line = bfr.readLine()) != null) {
            // this is a single line
            // example 017 # 0 # 0 # plant, flora, plant life # plants # ""
            String[] column = line.split("#");

            for (int i = 0; i < column.length; i++) {
                column[i] = column[i].trim();
            }

            // Column[4] Raw name
            String[] rawClasses = column[4].split(",");
            for (String eachClass : rawClasses) {

                eachClass = eachClass.trim().replace(" ", "_");
                eachClass = eachClass.substring(0, 1).toUpperCase() + eachClass.substring(1);

                // cache it
                allObjectsInADE20K.add(eachClass);
                /**
                 * Does this class name has any matching page name/concept name in wikipedia ?
                 * if it has then must assign the individual,
                 * otherwise there is no need to assign any types for this version of ontology.
                 */
                if (wikiConceptsHashSet.contains(eachClass)) {
                    matchedObjectswithWikiConcepts.add(eachClass);

                    // create object/namedIndividual
                    // column[0]
                    //String rawClassesNamesForObject = column[4].trim().replace(",", "_");
                    String instanceName = "obj_" + column[0] + "_" + column[1] + "_" + imageName;// + "_" + rawClassesNamesForObject;
                    iriIndi = IRI.create(ConfigParams.namespace + "#" + instanceName);
                    OWLNamedIndividual namedIndiObject = owlDataFactory.getOWLNamedIndividual(iriIndi);
                    // Assign this objects to the image by using imagecontains object property
                    OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = owlDataFactory
                            .getOWLObjectPropertyAssertionAxiom(owlObjPropImageContains, namedIndiImage, namedIndiObject);
                    addAxiom = new AddAxiom(ontology, owlObjectPropertyAssertionAxiom);
                    owlManager.applyChange(addAxiom);

                    // create class. if the IRI of 2 entities are same, then that will be a single entity in ontology and
                    // we will be able to access that from wiki ontology.
                    IRI iriClass = IRI.create(ConfigParams.namespace + "#" + eachClass);
                    OWLClass owlClass = owlDataFactory.getOWLClass(iriClass);

                    // assign individual to class
                    owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlClass, namedIndiObject);
                    addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
                    owlManager.applyChange(addAxiom);
                }

            }
        }

        // Save Ontology
        // for some reason this was creating problem on Jun,13,2020
        // owlManager.saveOntology(ontology, new OWLXMLDocumentFormat(), owlDiskFileIRI);
//        owlManager.saveOntology(ontology, owlDiskFileIRI);
        System.out.println("ontology has total " + ontology.getAxioms().size() + " axioms");
        System.out.println("saved on file: " + owlDiskFileIRI + "\nSuccessfull");
    }
}
