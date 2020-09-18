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
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Process wiki category from the db dump and make the category hierarchy ontology, without breaking the cycle.
 *
 * This creates all the possible hierarchy and all possible individuals.
 * As there is no starting point it also includes all concept including https://en.wikipedia.org/wiki/Category:Contents
 * which includes a lot of administrative information
 */
public class WikiCatsCyclicWithPages {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSetPages = null;
    private ResultSet resultSetCat = null;
    private String rootCategoryName = "Main_topic_classifications";

    private HashMap<Long, String> pageIdToTitleMap = new HashMap<>();
    private HashMap<Long, Integer> pageIdToNamespaceTypeMap = new HashMap<>();
    private HashMap<Long, HashSet<String>> catIdToParentsCat = new HashMap<>();

    private OWLOntology owlOntology;
    private TurtleDocumentFormat turtleDocumentFormat;
    private OWLXMLDocumentFormat owlxmlDocumentFormat;
    private RDFXMLDocumentFormat rdfxmlDocumentFormat;
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager owlOntologyManager;
    private int counter = 0;
    private int assertionCounter = 0;
    private int relationCounter = 0;

    String pathToSave = "../data/wiki_v3_";
    String onto_prefix = "http://www.daselab.org/ontologies/wiki#";

    public void fillCatHierHashMap(ResultSet resultSetCat) {
        try {
            while (resultSetCat.next()) {
                Long id = resultSetCat.getLong("cl_from");
                String name = resultSetCat.getString("cl_to");
                if (null != name) {
                    name = beautifyName(name);
                    if (catIdToParentsCat.containsKey(id)) {
                        catIdToParentsCat.get(id).add(name);
                    } else {
                        HashSet namesHashSet = new HashSet();
                        namesHashSet.add(name);
                        catIdToParentsCat.put(id, namesHashSet);
                    }
                } else {
                    System.out.println("category cl_to is null for id: " + id + " skipping");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

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
     * Read data from database and cache it
     */
    public void readDataBase() throws Exception {
        try {
            // This will load the MySQL driver, each DB has its own driver
//            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            System.out.println("connecting to db................ ");
            connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/wiki_cats", "****", "****");
            // Statements allow to issue SQL queries to the database
            statement = connect.createStatement();
            System.out.println("connecting to db successfull");

            // Result set get the result of the SQL query
            String catLinksQuery; // = "select cl_from, cl_to from wiki_cats.categorylinks where wiki_cats.categorylinks.cl_type='subcat'";
            catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where " +
                    " wiki_cats.categorylinks.cl_type='subcat' or wiki_cats.categorylinks.cl_type='page'";

            System.out.println("Query '" + catLinksQuery + "' started.............. ");
            resultSetCat = statement.executeQuery(catLinksQuery);
            System.out.println("Query '" + catLinksQuery + "'  successfull. ");
            System.out.println("fillCatHierHashMap started..........");
            fillCatHierHashMap(resultSetCat);
            System.out.println("fillCatHierHashMap finished. size of hashMap " + catIdToParentsCat.size());

            String pageTitleQuery = "select page_id, page_title from wiki_pages.page where wiki_pages.page.page_namespace=14" +
                    "or wiki_pages.page.page_namespace=0";
            System.out.println("Query '" + pageTitleQuery + "' started.............. ");
            resultSetPages = statement.executeQuery(pageTitleQuery);
            System.out.println("Query '" + pageTitleQuery + "' successfull. ");
            System.out.println("fillIdToTitleHashMap started..........");
            fillIdToTitleHashAndNamespaceMap(resultSetPages);
            System.out.println("fillIdToTitleHashMap finished. size of hashMap " + pageIdToTitleMap.size());

//            /*
//            Fill the hashMaps
//             */
//            fillHashMaps();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            closeConnections();
        }
    }

    // You need to closeConnections the resultSet
    private void closeConnections() {
        try {
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
        } catch (Exception e) {

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
     * Create relations and type assertions using the cached hashMap data
     *
     * This creates all the possible hierarchy and all possible individuals.
     * As there is no starting point it also includes all concept including https://en.wikipedia.org/wiki/Category:Contents
     * which includes a lot of administrative information
     */
    private void traverseDataBaseWithoutConstraint() {
        System.out.println("\nCreate relations started................ ");
        System.out.println("\ncatIdToParentsCat size: " + catIdToParentsCat.size());
        System.out.println("\npageIdToTitleMap size: " + pageIdToTitleMap.size());
        // iterate over the category
        for (Map.Entry<Long, HashSet<String>> longHashSetEntry : catIdToParentsCat.entrySet()) {
            // Child id and title
            Long child_Id = longHashSetEntry.getKey();
            String childTitle = pageIdToTitleMap.get(child_Id);

            if (null != childTitle) {
                Integer childNamespaceType = pageIdToNamespaceTypeMap.get(child_Id);

                if (null != childNamespaceType) {
                    // iterate over parent-categories
                    for (String parentCategoryName : longHashSetEntry.getValue()) {
                        if (childNamespaceType == 14) {
                            IRI child_IRI = IRI.create(onto_prefix + childTitle);
                            OWLClass child_Class = owlDataFactory.getOWLClass(child_IRI);

                            createRelation(child_Class, parentCategoryName);
                        } else if (childNamespaceType == 0) {
                            // this child must be a page, by definition, so we will create an individual here.
                            createTypeAssertion(childTitle, parentCategoryName);
                        }
                    }
                }
            } else {
                System.out.println("Page title not found for page_id: " + longHashSetEntry.getKey() + " skipping this category");
            }
            counter++;
        }
        System.out.println("create relations finished");
    }

    /**
     * Create relations and type assertions using the cached hashMap data
     *
     * This creates all the possible hierarchy and all possible individuals from the starting of
     * https://en.wikipedia.org/wiki/Category:Main_topic_classifications using BFS manner.
     * It doesn't detect cycle, just set the starting point and let it go.
     */
//    public void traverseDataBaseBFSWithoutConstraint() throws Exception {
//
//        String catLinksQuery = "";
//
//        try {
//
//            LinkedHashSet<String> catsQueue = new LinkedHashSet<>();
////            HashSet<String> visitedCatTitlesSet = new HashSet<>();
//            catsQueue.add(rootCategoryName);
//            int level = 0;
//
//            System.out.println("BFS started from the root node: " + rootCategoryName);
//            while (!catsQueue.isEmpty()) {
//
//                String parentCategoryName = catsQueue.iterator().next();
//                catsQueue.remove(parentCategoryName);
//
//                if (!visitedCatTitlesSet.contains(parentCategoryName)) {
//
//                    try {
//                        visitedCatTitlesSet.add(parentCategoryName);
//                        if (parentCategoryName.contains("'")) {
//                            catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where " +
//                                    "wiki_cats.categorylinks.cl_to=\"" + parentCategoryName + "\" and " +
//                                    "( wiki_cats.categorylinks.cl_type=\"subcat\" or " +
//                                    " wiki_cats.categorylinks.cl_type=\"page\" )";
//                        } else {
//                            catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where " +
//                                    "wiki_cats.categorylinks.cl_to='" + parentCategoryName + "' and " +
//                                    "( wiki_cats.categorylinks.cl_type=\"subcat\" or " +
//                                    " wiki_cats.categorylinks.cl_type=\"page\" )";
//                        }
//                        if (counter % 100000 == 0)
//                            System.out.println("Counter: " + counter + "\t Queue size: " + catsQueue.size() +
//                                    "\t Visited size: " + visitedCatTitlesSet.size() + "\t Query '" + catLinksQuery + "' started.............. ");
//                        resultSetCat = statement.executeQuery(catLinksQuery);
//                        if (counter % 100000 == 0)
//                            System.out.println("Counter: " + counter + "\t Queue size: " + catsQueue.size() +
//                                    "\t Visited size: " + visitedCatTitlesSet.size() + "\t Query '" + catLinksQuery + "'  successfull. ");
//
//                        // cache the resultSetCat
//                        HashSet<Long> childIdsHashSet = new HashSet<>();
//                        while (resultSetCat.next()) {
//                            Long child_Id = resultSetCat.getLong("cl_from");
//                            if (null != child_Id)
//                                childIdsHashSet.add(child_Id);
//                        }
//
//                        // get all children of this parent, parentCategoryName
//                        for (Long child_Id : childIdsHashSet) {
//
//                            String childCategoryName = pageIdToTitleMap.get(child_Id);
//                            Integer childNamespaceType = pageIdToNamespaceTypeMap.get(child_Id);
//                            if (null != childNamespaceType && null != childCategoryName) {
//                                if (childNamespaceType == 14) {
//                                    // we are breaking cycles, so we are not allowing relation with already visited node as child node.
//                                    if (!visitedCatTitlesSet.contains(childCategoryName)) {
//
//                                        // create relation, actually we are not creating hierarchy for non-cylcic,
//                                        // we are just creating the individuals and their immediate type. Later we need to combine this pages.owl
//                                        // with the cats.owl to get full owl
//                                        // createRelation(childCategoryName, parentCategoryName);
//
//                                        // do we need to visit this child? yes: as it's not in visitedCatTitlesHashSet
//                                        // if we directly add it to catsQueue then we may have duplicate copy of a category on the running/processing queue
//                                        if (!catsQueue.contains(childCategoryName))
//                                            catsQueue.add(childCategoryName);
//                                    } else {
//                                        missedCyclicCounter++;
//                                    }
//                                } else if (childNamespaceType == 0) {
//                                    // this child must be a page, by definition, so we will create an individual here.
//                                    createTypeAssertion(childCategoryName, parentCategoryName);
////                                    System.out.println("Indiv created ");
//                                }
//                            }
//
//                            counter++;
////                                if (counter > 200)
////                                    return;
//                        }
//                    } catch (SQLException ex) {
//                        System.out.println("Exception in executing query....skipping this query");
//                        System.out.println("catlinkQuery: " + catLinksQuery);
////                        System.out.println("pageTitleQuery: " + pageTitleQuery);
//                    }
//                }
//            }
//
//            System.out.println("BFS finished. counter " + counter);
//        } catch (Exception e) {
//            System.out.println("Exception!!!!!!!!!");
//            System.out.println("catlinkQuery: " + catLinksQuery);
////            System.out.println("pageTitleQuery: " + pageTitleQuery);
//            e.printStackTrace();
//        } finally {
////            closeConnections();
//        }
//    }


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

    /**
     * create a single subclassOf relation
     *
     * @param cClass
     * @param pName
     */
    private void createRelation(OWLClass cClass, String pName) {
        IRI pIRI = IRI.create(onto_prefix + pName);

        OWLClass pClass = owlDataFactory.getOWLClass(pIRI);
        OWLAxiom owlAxiom = owlDataFactory.getOWLSubClassOfAxiom(cClass, pClass);

        owlOntologyManager.addAxiom(owlOntology, owlAxiom);
    }

    public void saveOntoToFile() {
        String finalPathToSave = pathToSave + counter + ".rdf";
        System.out.println("\nSaving to " + finalPathToSave + " started...........");
        try {
            // there was a problem when saving into turtle format.
            Utility.saveOntology(owlOntology, rdfxmlDocumentFormat, finalPathToSave);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
        System.out.println("\nSaving to " + finalPathToSave + " successfull");
    }

    public void initData() {
        owlOntologyManager = OWLManager.createOWLOntologyManager();
        try {
            owlOntology = owlOntologyManager.createOntology(IRI.create("http://www.daselab.org/ontologies/wiki"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        owlDataFactory = owlOntologyManager.getOWLDataFactory();
        turtleDocumentFormat = new TurtleDocumentFormat();
        owlxmlDocumentFormat = new OWLXMLDocumentFormat();
        rdfxmlDocumentFormat = new RDFXMLDocumentFormat();
    }

    /**
     * Calculate object size
     *
     * @param o
     */
    public void calcObjectSize(Object o) {
//        Instrumentation instrumentation = new
        // https://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object
    }

    public static void main(String[] args) throws Exception {
        WikiCatsCyclicWithPages wikiCatsCyclic = new WikiCatsCyclicWithPages();
        wikiCatsCyclic.initData();

        final long readDatabaseStartTime = System.currentTimeMillis();
        wikiCatsCyclic.readDataBase();
        final long readDatabaseEndTime = System.currentTimeMillis();
        System.out.println("Databse read+cache time: " + (readDatabaseEndTime - readDatabaseStartTime) / 60000 + " minutes");

        final long createRelationStartTime = System.currentTimeMillis();
        wikiCatsCyclic.traverseDataBaseWithoutConstraint();
        final long createRelationEndTime = System.currentTimeMillis();
        System.out.println("Create relations time: " + (createRelationEndTime - createRelationStartTime) / 60000 + " minutes");

        final long saveOntologyStartTime = System.currentTimeMillis();
        wikiCatsCyclic.saveOntoToFile();
        final long saveOntologyEndTime = System.currentTimeMillis();
        System.out.println("Save ontology time: " + (saveOntologyEndTime - saveOntologyStartTime) / 60000 + " minutes");
    }

}