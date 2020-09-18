package analysis;
/*
Written by sarker.
Written at 1/26/20.
*/

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import Util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class WikiKGStatsJena {

    private OntModel ontModel;

    private OWLOntology owlOntology;
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager owlOntologyManager;
    private String ontoPath = "../data/wiki_full_pages_v0_non_cyclic_jan_20_32808131.rdf";
    private String onto_prefix = "http://www.daselab.com/residue/analysis#";
    private OWLClass rootClass;
    private OWLReasoner owlReasoner;

    public void calcStats() {
        System.out.println("Total axioms: " + owlOntology.getAxiomCount());
        System.out.println("Total classes: " + owlOntology.getClassesInSignature().size());
        System.out.println("Total subclassOf axioms: " + owlOntology.getAxiomCount(AxiomType.SUBCLASS_OF));
        System.out.println("Total individuals: " + owlOntology.getIndividualsInSignature().size());
        System.out.println("Total objectProperties: " + owlOntology.getObjectPropertiesInSignature().size());
    }

    public void createTree() {
        IRI rootClassIRI = IRI.create(onto_prefix + "Main_topic_classifications");
        rootClass = owlDataFactory.getOWLClass(rootClassIRI);
    }

    public void getRootClass() {
        IRI rootClassIRI = IRI.create(onto_prefix + "Main_topic_classifications");
        rootClass = owlDataFactory.getOWLClass(rootClassIRI);
        if (owlOntology.containsClassInSignature(rootClassIRI)) {
            System.out.println("Ontology has root class " + rootClass.getIRI().toString());
        } else {
            System.out.println("Could not find root class");
        }
    }

    public void doReasoning() {
        if (null != owlOntology) {
            System.out.println("Reasoning starting .....................");
            owlReasoner = Utility.initReasoner("pellet", owlOntology, null);
            System.out.println("Reasoning finished");
        }
    }


    public void initOnto() {
        try {


            System.out.println("Loading ontology .....................");
            ontModel = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
            ontModel.read("file:" + ontoPath);
//            ontModel.getOntClass("").listSubClasses(true);
//            owlOntology = Utility.loadOntology(ontoPath);
            System.out.println("Loading ontology finished");
            owlOntologyManager = owlOntology.getOWLOntologyManager();
            owlDataFactory = owlOntologyManager.getOWLDataFactory();
        } catch (Exception ex) {

        }
    }

    public static void main(String[] args) {

        WikiKGStatsJena wikiKGStats = new WikiKGStatsJena();

        final long intiOntoStartTime = System.currentTimeMillis();
        wikiKGStats.initOnto();
        final long intiOntoEndTime = System.currentTimeMillis();
        System.out.println("Init ontology time: " + (intiOntoEndTime - intiOntoStartTime) / 60000 + " minutes");

//        wikiKGStats.calcStats();

//        final long reasoningStartTime = System.currentTimeMillis();
//        wikiKGStats.doReasoning();
//        final long reasoningEndTime = System.currentTimeMillis();
//        System.out.println("Reasoning time: " + (reasoningEndTime - reasoningStartTime) / 60000 + " minutes");
//
//        final long findRootStartTime = System.currentTimeMillis();
//        wikiKGStats.getRootClass();
//        final long findRootEndTime = System.currentTimeMillis();
//        System.out.println("Find root time: " + (findRootEndTime - findRootStartTime) / 60000 + " minutes");

    }
}
