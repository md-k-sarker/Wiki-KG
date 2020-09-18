package wiki.creation.datadump;
/*
Written by sarker.
Written at 11/16/19.
*/


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import Util.Utility;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
 * Strictly use the BFS method
 */
public class WikiCatsNoCycle {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSetPages = null;
    private ResultSet resultSetCat = null;
    private String rootCategoryName = "Main_topic_classifications";
    private String rootCategoryNameIncludingAdminInfo = "Contents";

    private HashMap<Long, String> pageIdToTitleMap = new HashMap<>();
    private HashMap<Long, HashSet<String>> catIdToParentsCat = new HashMap<>();

    private OWLOntology owlOntology;
    //    private TurtleDocumentFormat turtleDocumentFormat;
    private OWLXMLDocumentFormat owlxmlDocumentFormat;
    private RDFXMLDocumentFormat rdfxmlDocumentFormat;
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager owlOntologyManager;
    private int counter = 0;

    /**
     * Parent,Child. If we would have created this link then it would create a cycle.
     */
    HashMap<String, String> cyclicRelationCutterMap = new HashMap<>();

    String pathToSave = "../data/wiki_full_cats_v0_non_cyclic_jan_20_";
    String onto_prefix = "http://www.daselab.org/ontologies/wiki#";


    /**
     *
     */
    public void cachePageTitles() {
        try {
            String pageTitleQuery = "select page_id, page_title from wiki_pages.page where wiki_pages.page.page_namespace=14";
            System.out.println("Query '" + pageTitleQuery + "' started.............. ");
            resultSetPages = statement.executeQuery(pageTitleQuery);
            System.out.println("Query '" + pageTitleQuery + "' successfull. ");
            System.out.println("fillIdToTitleHashMap started..........");
            fillIdToTitleHashMap(resultSetPages);
            System.out.println("fillIdToTitleHashMap finished. size of hashMap " + pageIdToTitleMap.size());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param resultSetPages
     */
    public void fillIdToTitleHashMap(ResultSet resultSetPages) {
        System.out.println("initially pageIdToTitleMap size: "+ pageIdToTitleMap.size());
        try {
            while (resultSetPages.next()) {
                Long page_id = resultSetPages.getLong("page_id");
                String page_title = resultSetPages.getString("page_title");
                if (null != page_id && null != page_title) {
                    pageIdToTitleMap.put(page_id, beautifyName(page_title));
                } else {
                    System.out.println("ID or Title is null, skipping.. id: " + page_id + "\t title: " + page_title);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        System.out.println("after filling pageIdToTitleMap size: "+ pageIdToTitleMap.size());
    }

    public void saveCyclicInfoToCSV() {
        try {
            String csv_path = "../data/cyclic_breaker.csv";
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(csv_path));
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.DEFAULT.withHeader("Parent", "Child"));

            cyclicRelationCutterMap.forEach((parent, child) -> {
                try {
                    csvPrinter.printRecord(parent,child);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    public void traverseDataBaseBFS() throws Exception {

        String catLinksQuery = "";
        String pageTitleQuery = "";

        try {

            LinkedHashSet<String> catsQueue = new LinkedHashSet<>();
            HashSet<String> visitedCatTitlesHashSet = new HashSet<>();
            catsQueue.add(rootCategoryName);
            int level = 0;

            System.out.println("BFS started from the root node: " + rootCategoryName);
            while (!catsQueue.isEmpty()) {

                String parentCategoryName = catsQueue.iterator().next();
                catsQueue.remove(parentCategoryName);
//                counter++;
                if (!visitedCatTitlesHashSet.contains(parentCategoryName)) {

                    try {
                        visitedCatTitlesHashSet.add(parentCategoryName);
                        if (parentCategoryName.contains("'")) {
                            catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where " +
                                    "wiki_cats.categorylinks.cl_to=\"" + parentCategoryName + "\" and wiki_cats.categorylinks.cl_type=\"subcat\"";
                        } else {
                            catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where " +
                                    "wiki_cats.categorylinks.cl_to='" + parentCategoryName + "' and wiki_cats.categorylinks.cl_type=\"subcat\"";
                        }
                        if (counter % 100000 == 0)
                            System.out.println("Counter: " + counter + "\t Queue size: " + catsQueue.size() +
                                    "\t Visited size: " + visitedCatTitlesHashSet.size() + "\t Query '" + catLinksQuery + "' started.............. ");
                        resultSetCat = statement.executeQuery(catLinksQuery);
                        if (counter % 100000 == 0)
                            System.out.println("Counter: " + counter + "\t Queue size: " + catsQueue.size() +
                                    "\t Visited size: " + visitedCatTitlesHashSet.size() + "\t Query '" + catLinksQuery + "'  successfull. ");

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
                            if (null != childCategoryName) {
                                // create relation
                                // we are breaking cycles, so we are not allowing relation  with already visited node as child node.
                                if (!visitedCatTitlesHashSet.contains(childCategoryName)) {
                                    createRelation(childCategoryName, parentCategoryName);
                                    // do we need to visit this child? yes: as it's not in visitedCatTitlesHashSet
                                    // if we directly add it to catsQueue then we may have duplicate copy of a category on the running/processing queue
                                    if (!catsQueue.contains(childCategoryName))
                                        catsQueue.add(childCategoryName);
                                }
                            }
                            counter++;
//                                if (counter > 200)
//                                    return;
                        }
                    } catch (SQLException ex) {
                        System.out.println("Exception in executing query....skipping this query");
                        System.out.println("catlinkQuery: " + catLinksQuery);
                        System.out.println("pageTitleQuery: " + pageTitleQuery);
                    }
                }
            }

            System.out.println("BFS finished. counter " + counter);
        } catch (Exception e) {
            System.out.println("Exception!!!!!!!!!");
            System.out.println("catlinkQuery: " + catLinksQuery);
            System.out.println("pageTitleQuery: " + pageTitleQuery);
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
    }

    public void saveOntoToFile() {
        String finalPathToSave = pathToSave + counter + ".owl";
        System.out.println("\nSaving to " + finalPathToSave + " started...........");
        try {
            Utility.saveOntology(owlOntology, owlxmlDocumentFormat, finalPathToSave);
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

    public static void main(String[] args) throws Exception {
        WikiCatsNoCycle wikiCatsNoCycle = new WikiCatsNoCycle();
        wikiCatsNoCycle.initData();

        final long readDatabaseStartTime = System.currentTimeMillis();
        wikiCatsNoCycle.cachePageTitles();
        wikiCatsNoCycle.traverseDataBaseBFS();
        final long readDatabaseEndTime = System.currentTimeMillis();
        System.out.println("Databse read+traverse time: " + (readDatabaseEndTime - readDatabaseStartTime) / 60000 + " minutes");

        wikiCatsNoCycle.closeConnections();

        final long saveOntologyStartTime = System.currentTimeMillis();
        wikiCatsNoCycle.saveCyclicInfoToCSV();
        final long saveOntologyEndTime = System.currentTimeMillis();
        System.out.println("Save cyclic info time: " + (saveOntologyEndTime - saveOntologyStartTime) / 60000 + " minutes");
    }

}