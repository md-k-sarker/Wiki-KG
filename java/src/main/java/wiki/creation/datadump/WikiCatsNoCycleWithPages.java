package wiki.creation.datadump;
/*
Written by sarker.
Written at 11/16/19.
*/


import Util.Utility;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

//import org.semanticweb.owlapi.formats.TurtleDocumentFormat;

/**
 * Signature of subclass : getOWLSubClassOfAxiom(OWLClassExpression subClass, OWLClassExpression superClass)
 */

/**
 * Process wiki category from the db dump and make the category hierarchy ontology, while breaking the cycle.
 */
public class WikiCatsNoCycleWithPages {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSetPages = null;
    private ResultSet resultSetCat = null;
    private String rootCategoryName = "Main_topic_classifications";
    private String rootCategoryNameIncludingAdminInfo = "Contents";

    private HashMap<Long, String> pageIdToTitleMap = new HashMap<>();
    private HashMap<Long, Integer> pageIdToNamespaceTypeMap = new HashMap<>();
    private HashMap<Long, HashSet<String>> catIdToParentsCat = new HashMap<>();

    private OWLOntology owlOntology;
    //    private TurtleDocumentFormat turtleDocumentFormat;
    private OWLXMLDocumentFormat owlxmlDocumentFormat;
    private RDFXMLDocumentFormat rdfxmlDocumentFormat;
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager owlOntologyManager;
    private int counter = 0;
    private int assertionCounter = 0;
    private int relationCounter = 0;
    /**
     * cl_from, cl_to gives all possible category links where a portal (namespace=100) page is also included. But we are only concerned about the namespace=0
     */
    private int missedAssertionCounter = 0;
    /**
     * Counter to count the no. of relation, which was cut to maintain tree instead of graph relation.
     */
    private int missedCyclicCounter = 0;


    // String pathToSaveCategory = "../data/wiki_full_cats_with_pages_v0_non_cyclic_jan_20_";
    String pathToSaveIndivs = "../data/wiki_full_pages_v0_non_cyclic_jan_20_";
    String onto_prefix = "http://www.daselab.com/residue/analysis#";

    public void cachePageTitlesAndNamespaces() {
        try {
            String pageTitleQuery = "select page_id, page_title, page_namespace from wiki_pages.page where wiki_pages.page.page_namespace=14 " +
                    "or wiki_pages.page.page_namespace=0";
            System.out.println("Query '" + pageTitleQuery + "' started.............. ");
            resultSetPages = statement.executeQuery(pageTitleQuery);
            System.out.println("Query '" + pageTitleQuery + "' successfull. ");
            System.out.println("fillIdToTitleHashAndNamespaceMap started..........");
            fillIdToTitleHashAndNamespaceMap(resultSetPages);
            System.out.println("fillIdToTitleHashAndNamespaceMap finished. size of hashMap " + pageIdToTitleMap.size());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param resultSetPages
     */
    public void fillIdToTitleHashAndNamespaceMap(ResultSet resultSetPages) {
        System.out.println("initially pageIdToTitleMap size: " + pageIdToTitleMap.size());
        System.out.println("initially pageIdToNamespaceTypeMap size: " + pageIdToNamespaceTypeMap.size());
        try {
            while (resultSetPages.next()) {
                Long page_id = resultSetPages.getLong("page_id");
                Integer page_namespace = resultSetPages.getInt("page_namespace");
                String page_title = resultSetPages.getString("page_title");
                if (null != page_id) {
                    if (null != page_title) {
                        pageIdToTitleMap.put(page_id, beautifyName(page_title));
                    } else {
                        System.out.println(" title is null, skipping.. id: " + page_id + "\t title: " + page_title);
                    }
                    if (null != page_namespace) {
                        pageIdToNamespaceTypeMap.put(page_id, page_namespace);
                    } else {
                        System.out.println("page_name space is null, skipping.. page_namespace: " + page_id + "\t title: " + page_title);
                    }
                } else {
                    System.out.println("ID is null, skipping.. id: " + page_id);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("after filling pageIdToTitleMap size: " + pageIdToTitleMap.size());
        System.out.println("after filling pageIdToNamespaceTypeMap size: " + pageIdToNamespaceTypeMap.size());
    }


    /**
     *
     */
    public void traverseDataBaseBFS() throws Exception {

        String catLinksQuery = "";
//        String pageTitleQuery = "";

        try {

            LinkedHashSet<String> catsQueue = new LinkedHashSet<>();
            HashSet<String> visitedCatTitles = new HashSet<>();
            catsQueue.add(rootCategoryName);
            int level = 0;

            System.out.println("BFS started from the root node: " + rootCategoryName);
            while (!catsQueue.isEmpty()) {

                String parentCategoryName = catsQueue.iterator().next();
                catsQueue.remove(parentCategoryName);
//                counter++;
                if (!visitedCatTitles.contains(parentCategoryName)) {

                    try {
                        visitedCatTitles.add(parentCategoryName);
                        if (parentCategoryName.contains("'")) {
                            catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where " +
                                    "wiki_cats.categorylinks.cl_to=\"" + parentCategoryName + "\" and " +
                                    "( wiki_cats.categorylinks.cl_type=\"subcat\" or " +
                                    " wiki_cats.categorylinks.cl_type=\"page\" )";
                        } else {
                            catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where " +
                                    "wiki_cats.categorylinks.cl_to='" + parentCategoryName + "' and " +
                                    "( wiki_cats.categorylinks.cl_type=\"subcat\" or " +
                                    " wiki_cats.categorylinks.cl_type=\"page\" )";
                        }
                        if (counter % 100000 == 0)
                            System.out.println("Counter: " + counter + "\t Queue size: " + catsQueue.size() +
                                    "\t Visited size: " + visitedCatTitles.size() + "\t Query '" + catLinksQuery + "' started.............. ");
                        resultSetCat = statement.executeQuery(catLinksQuery);
                        if (counter % 100000 == 0)
                            System.out.println("Counter: " + counter + "\t Queue size: " + catsQueue.size() +
                                    "\t Visited size: " + visitedCatTitles.size() + "\t Query '" + catLinksQuery + "'  successfull. ");

                        // cache the resultSetCat
                        HashSet<Long> childIdsHashSet = new HashSet<>();
                        while (resultSetCat.next()) {
                            Long child_Id = resultSetCat.getLong("cl_from");
                            if (null != child_Id)
                                childIdsHashSet.add(child_Id);
                        }

                        // get all children of this parent, parentCategoryName
                        for (Long child_Id : childIdsHashSet) {

                            String childCategoryName = pageIdToTitleMap.get(child_Id);
                            Integer childNamespaceType = pageIdToNamespaceTypeMap.get(child_Id);
                            if (null != childNamespaceType && null != childCategoryName) {
                                if (childNamespaceType == 14) {
                                    // we are breaking cycles, so we are not allowing relation with already visited node as child node.
                                    if (!visitedCatTitles.contains(childCategoryName)) {

                                        // create relation, actually we are not creating hierarchicy for non-cylcic,
                                        // we are just creating the individuals and their immediate type. Later we need to combine this pages.owl
                                        // with the cats.owl to get full owl
                                        // createRelation(childCategoryName, parentCategoryName);

                                        // do we need to visit this child? yes: as it's not in visitedCatTitlesHashSet
                                        // if we directly add it to catsQueue then we may have duplicate copy of a category on the running/processing queue
                                        if (!catsQueue.contains(childCategoryName))
                                            catsQueue.add(childCategoryName);
                                    } else {
                                        missedCyclicCounter++;
                                    }
                                } else if (childNamespaceType == 0) {
                                    // this child must be a page, by definition, so we will create an individual here.
                                    createTypeAssertion(childCategoryName, parentCategoryName);
//                                    System.out.println("Indiv created ");
                                }
                            } else {
                                /**
                                 * cl_from, cl_to gives all possible category links where a portal (namespace=100) page is also included.
                                 * But we are only concerned about the namespace=0, so omitting this kind of assertion
                                 *
                                 * this condition may happen a lot of time.
                                 */
                                missedAssertionCounter++;
//                                System.out.println("childNamespaceType is null or childCategoryName is null ");
//                                System.out.println("null result....parentCategory: " + parentCategoryName + "  childId:" + child_Id);
//                                try {
//                                    System.out.println("null childNamespaceType: " + childNamespaceType);
//                                } catch (Exception ex) {
//
//                                }
//                                try {
//                                    System.out.println("null childCategoryName: " + childCategoryName);
//                                } catch (Exception ex) {
//
//                                }
                            }

                            counter++;
//                                if (counter > 200)
//                                    return;
                        }
                    } catch (SQLException ex) {
                        System.out.println("Exception in executing query....skipping this query");
                        System.out.println("catlinkQuery: " + catLinksQuery);
//                        System.out.println("pageTitleQuery: " + pageTitleQuery);
                    }
                }
            }

            System.out.println("BFS finished. counter " + counter);
        } catch (Exception e) {
            System.out.println("Exception!!!!!!!!!");
            System.out.println("catlinkQuery: " + catLinksQuery);
//            System.out.println("pageTitleQuery: " + pageTitleQuery);
            e.printStackTrace();
        } finally {
//            closeConnections();
        }
    }

    // You need to closeConnections the resultSet
    private void closeConnections() {
        try {
            System.out.println("Closing db connections..........");
            if (resultSetPages != null) {
                resultSetPages.close();
            }
            if (resultSetCat != null) {
                resultSetCat.close();
            }
            if (statement != null) {
                statement.close();
            }

            if (connect != null) {
                connect.close();
            }
            System.out.println("Closing db connections successfull");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String trimOrReplaceSearchChars = " `~!@#$%^&*()-+={}[]|\\;'\"<>,.?/";
    // length of replaceChars must be same with trimOrReplaceSearchChars
    // https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/StringUtils.html#replaceChars(java.lang.String,%20java.lang.String,%20java.lang.String)
    private String replaceChars = "_______________________________";

    public String beautifyName(String name) {
        String trimmed = StringUtils.strip(name, trimOrReplaceSearchChars);
        return StringUtils.replaceChars(trimmed, trimOrReplaceSearchChars, replaceChars);
    }

    /**
     * create a single subclassOf relation
     *
     * @param childName
     * @param parentName
     */
    private void createRelation(String childName, String parentName) {
        IRI cIRI = IRI.create(onto_prefix + beautifyName(childName));
        IRI pIRI = IRI.create(onto_prefix + beautifyName(parentName));

        OWLClass cClass = owlDataFactory.getOWLClass(cIRI);
        OWLClass pClass = owlDataFactory.getOWLClass(pIRI);

        OWLAxiom owlAxiom = owlDataFactory.getOWLSubClassOfAxiom(cClass, pClass);
        owlOntologyManager.addAxiom(owlOntology, owlAxiom);
        relationCounter++;
    }

    /**
     * create a single Type relation
     *
     * @param childName
     * @param parentName
     */
    private void createTypeAssertion(String childName, String parentName) {
        IRI cIRI = IRI.create(onto_prefix + beautifyName(childName));
        IRI pIRI = IRI.create(onto_prefix + beautifyName(parentName));

        OWLNamedIndividual cIndiv = owlDataFactory.getOWLNamedIndividual(cIRI);
        OWLClass pClass = owlDataFactory.getOWLClass(pIRI);

        OWLAxiom owlAxiom = owlDataFactory.getOWLClassAssertionAxiom(pClass, cIndiv);
        owlOntologyManager.addAxiom(owlOntology, owlAxiom);
        assertionCounter++;
    }

    public void saveOntoToFile() {
        String finalPathToSave = pathToSaveIndivs + counter + ".rdf";
        System.out.println("\nSaving to " + finalPathToSave + " started...........");
        try {
            Utility.saveOntology(owlOntology, rdfxmlDocumentFormat, finalPathToSave);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        System.out.println("\nSaving to " + finalPathToSave + " successfull");
    }

    public void initData() {
        owlOntologyManager = OWLManager.createOWLOntologyManager();
        try {
            owlOntology = owlOntologyManager.createOntology(IRI.create("http://www.daselab.com/residue/analysis"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        owlDataFactory = owlOntologyManager.getOWLDataFactory();
//        turtleDocumentFormat = new TurtleDocumentFormat();
        rdfxmlDocumentFormat = new RDFXMLDocumentFormat();
        owlxmlDocumentFormat = new OWLXMLDocumentFormat();
        try {
            System.out.println("connecting to db................ ");
            connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/wiki_cats", "****", "****");
            // Statements allow to issue SQL queries to the database
            statement = connect.createStatement();
            System.out.println("connecting to db successfull");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void indivStructureTest() {
        createTypeAssertion("Human", "Mammal");
    }

    public static void main(String[] args) throws Exception {
        WikiCatsNoCycleWithPages wikiCatsNoCycleWithPages = new WikiCatsNoCycleWithPages();
        wikiCatsNoCycleWithPages.initData();

        final long readDatabaseStartTime = System.currentTimeMillis();
        wikiCatsNoCycleWithPages.cachePageTitlesAndNamespaces();
        wikiCatsNoCycleWithPages.traverseDataBaseBFS();
        final long readDatabaseEndTime = System.currentTimeMillis();
        System.out.println("Databse read+traverse time: " + (readDatabaseEndTime - readDatabaseStartTime) / 60000 + " minutes");

        wikiCatsNoCycleWithPages.closeConnections();

//        wikiCatsNoCycleWithPages.indivStructureTest();

        final long saveOntologyStartTime = System.currentTimeMillis();
        wikiCatsNoCycleWithPages.saveOntoToFile();
        final long saveOntologyEndTime = System.currentTimeMillis();
        System.out.println("Save ontology time: " + (saveOntologyEndTime - saveOntologyStartTime) / 60000 + " minutes");

        System.out.println("counter: " + wikiCatsNoCycleWithPages.counter);
        System.out.println("relationCounter: " + wikiCatsNoCycleWithPages.relationCounter);
        System.out.println("assertionCounter: " + wikiCatsNoCycleWithPages.assertionCounter);
        System.out.println("missedAssertionCounter: " + wikiCatsNoCycleWithPages.missedAssertionCounter);
        System.out.println("missedCyclicCounter: " + wikiCatsNoCycleWithPages.missedCyclicCounter);
    }

}