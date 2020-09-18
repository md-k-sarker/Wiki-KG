package analysis;
/*
Written by sarker.
Written at 1/26/20.
*/

import Util.Utility;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class WikiKGStats {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    private OWLOntology owlOntology;
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager owlOntologyManager;
    private String ontoPath = "../data/wiki_pages_with_category_v1.rdf";
    private String onto_prefix = "http://www.daselab.org/ontologies/wiki#";
    private OWLClass rootClass;
    private OWLReasoner owlReasoner;

    public void calcStats() {
        logger.info("Total classes: " + owlOntology.getClassesInSignature().size());
        logger.info("Total individuals: " + owlOntology.getIndividualsInSignature().size());
        logger.info("Total objectProperties: " + owlOntology.getObjectPropertiesInSignature().size());
        logger.info("Total axioms: " + owlOntology.getAxiomCount());
        logger.info("Total classAssertion axioms: " + owlOntology.getAxiomCount(AxiomType.CLASS_ASSERTION));
        logger.info("Total subclassOf axioms: " + owlOntology.getAxiomCount(AxiomType.SUBCLASS_OF));
    }

    public void createTree() {
        IRI rootClassIRI = IRI.create(onto_prefix + "Main_topic_classifications");
        rootClass = owlDataFactory.getOWLClass(rootClassIRI);
    }

    public void getRootClass() {
        IRI rootClassIRI = IRI.create(onto_prefix + "Main_topic_classifications");
        rootClass = owlDataFactory.getOWLClass(rootClassIRI);
        if (owlOntology.containsClassInSignature(rootClassIRI)) {
            logger.info("Ontology has root class " + rootClass.getIRI().toString());
        } else {
            logger.info("Could not find root class");
        }
    }

    public void doReasoning() {
        if (null != owlOntology) {
            logger.info("Reasoning starting .....................");
            owlReasoner = Utility.initReasoner("pellet", owlOntology, null);
            logger.info("Reasoning finished");
        }
    }


    public void initOnto() {
        try {

            logger.info("Loading ontology .....................");
            owlOntology = Utility.loadOntology(ontoPath);
            logger.info("Loading ontology finished");
            owlOntologyManager = owlOntology.getOWLOntologyManager();
            owlDataFactory = owlOntologyManager.getOWLDataFactory();
        } catch (Exception ex) {

        }
    }

    /**
     * Assume ontology is already loaded
     */
    public void convertFromRDFtoOWL() {

        String outputPath = "../data/wiki_pages_with_category_v1.owl";
        try {
            logger.info("Saving ontology into " + outputPath + "  ......");
            Utility.saveOntology(owlOntology, outputPath);
            logger.info("Saving ontology finished");
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        WikiKGStats wikiKGStats = new WikiKGStats();
        
        
        final long intiOntoStartTime = System.currentTimeMillis();
        wikiKGStats.initOnto();
        final long initOntoEndTime = System.currentTimeMillis();
        logger.info("Init ontology time: " + (initOntoEndTime - intiOntoStartTime) / 60000 + " minutes");

        wikiKGStats.convertFromRDFtoOWL();

        final long saveOntoEndTime = System.currentTimeMillis();
        logger.info("Saving ontology time: " + (saveOntoEndTime - initOntoEndTime ) / 60000 + " minutes");

//        final long reasoningStartTime = System.currentTimeMillis();
//        wikiKGStats.doReasoning();
//        final long reasoningEndTime = System.currentTimeMillis();
//        logger.info("Reasoning time: " + (reasoningEndTime - reasoningStartTime) / 60000 + " minutes");
//
//        final long findRootStartTime = System.currentTimeMillis();
//        wikiKGStats.getRootClass();
//        final long findRootEndTime = System.currentTimeMillis();
//        logger.info("Find root time: " + (findRootEndTime - findRootStartTime) / 60000 + " minutes");

    }
}
