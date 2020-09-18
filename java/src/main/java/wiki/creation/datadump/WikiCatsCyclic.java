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
import java.util.Map;

/**
 * Process wiki category from the db dump and make the category hierarchy ontology, without breaking the cycle.
 */
public class WikiCatsCyclic {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSetPages = null;
    private ResultSet resultSetCat = null;

    private HashMap<Long, String> pageIdToTitleMap = new HashMap<>();
    private HashMap<Long, HashSet<String>> catIdToParentsCat = new HashMap<>();

    private OWLOntology owlOntology;
    private TurtleDocumentFormat turtleDocumentFormat;
    private OWLXMLDocumentFormat owlxmlDocumentFormat;
    private RDFXMLDocumentFormat rdfxmlDocumentFormat;
    private OWLDataFactory owlDataFactory;
    private OWLOntologyManager owlOntologyManager;
    private int counter = 0;

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

    /**
     * @param resultSetPages
     */
    public void fillIdToTitleHashMap(ResultSet resultSetPages) {
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
            String catLinksQuery = "select cl_from, cl_to from wiki_cats.categorylinks where wiki_cats.categorylinks.cl_type='subcat'";
            System.out.println("Query '" + catLinksQuery + "' started.............. ");
            resultSetCat = statement.executeQuery(catLinksQuery);
            System.out.println("Query '" + catLinksQuery + "'  successfull. ");
            System.out.println("fillCatHierHashMap started..........");
            fillCatHierHashMap(resultSetCat);
            System.out.println("fillCatHierHashMap finished. size of hashMap " + catIdToParentsCat.size());

            String pageTitleQuery = "select page_id, page_title from wiki_pages.page where wiki_pages.page.page_namespace=14";
            System.out.println("Query '" + pageTitleQuery + "' started.............. ");
            resultSetPages = statement.executeQuery(pageTitleQuery);
            System.out.println("Query '" + pageTitleQuery + "' successfull. ");
            System.out.println("fillIdToTitleHashMap started..........");
            fillIdToTitleHashMap(resultSetPages);
            System.out.println("fillIdToTitleHashMap finished. size of hashMap " + pageIdToTitleMap.size());

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
     * Create relations using the cached hashMap data
     */
    private void createRelations() {
        System.out.println("\nCreate relations started................ ");
        System.out.println("\ncatIdToParentsCat size: " + catIdToParentsCat.size());
        System.out.println("\npageIdToTitleMap size: " + pageIdToTitleMap.size());
        // iterate over the category
        for (Map.Entry<Long, HashSet<String>> longHashSetEntry : catIdToParentsCat.entrySet()) {
            // get title of the id
            String child_title = pageIdToTitleMap.get(longHashSetEntry.getKey());
            if (null != child_title) {
                IRI child_IRI = IRI.create(onto_prefix + child_title);
                OWLClass child_Class = owlDataFactory.getOWLClass(child_IRI);

                // iterate over parent-categories
                for (String parent_title : longHashSetEntry.getValue()) {
                    // make subclass of relation
                    createRelation(child_Class, parent_title);
                }
            } else {
                System.out.println("Page title not found for page_id: " + longHashSetEntry.getKey() + " skipping this category");
            }
            counter++;
//            if (counter > 10)
//                break;
        }
        System.out.println("create relations finished");
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
        WikiCatsCyclic wikiCatsCyclic = new WikiCatsCyclic();
        wikiCatsCyclic.initData();

        final long readDatabaseStartTime = System.currentTimeMillis();
        wikiCatsCyclic.readDataBase();
        final long readDatabaseEndTime = System.currentTimeMillis();
        System.out.println("Databse read+cache time: " + (readDatabaseEndTime - readDatabaseStartTime) / 60000 + " minutes");

        final long createRelationStartTime = System.currentTimeMillis();
        wikiCatsCyclic.createRelations();
        final long createRelationEndTime = System.currentTimeMillis();
        System.out.println("Create relations time: " + (createRelationEndTime - createRelationStartTime) / 60000 + " minutes");

        final long saveOntologyStartTime = System.currentTimeMillis();
        wikiCatsCyclic.saveOntoToFile();
        final long saveOntologyEndTime = System.currentTimeMillis();
        System.out.println("Save ontology time: " + (saveOntologyEndTime - saveOntologyStartTime) / 60000 + " minutes");
    }

}