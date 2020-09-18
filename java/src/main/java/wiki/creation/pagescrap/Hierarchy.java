//package wiki.creation.pagescrap;
///*
//Written by sarker.
//Written at 11/14/19.
//*/
//
//import org.apache.commons.io.FileUtils;
//import org.apache.log4j.PropertyConfigurator;
//import org.dase.ecii.util.Utility;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.model.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.lang.invoke.MethodHandles;
//import java.util.HashMap;
//
///**
// * Hierarchy retrievar
// */
//
///**
// * Example of page link:
// *           <div class="mw-category-group">
// *           <h3>J</h3>
// *           <ul>
// *            <li><a href="/wiki/Jurisprudence" title="Jurisprudence">Jurisprudence</a></li>
// *           </ul>
// *          </div>
// *
// *
// *  Example of category link:
// *     <div class="CategoryTreeSection">
// *              <div class="CategoryTreeItem">
// *               <span class="CategoryTreeBullet"><span class="CategoryTreeToggle" data-ct-title="Sustainable_development" data-ct-state="collapsed">►</span> </span>
// *               <a href="/wiki/Category:Sustainable_development" title="Category:Sustainable development">Sustainable development</a>‎
// *               <span title="Contains 12 subcategories, 83 pages, and 0 files" dir="ltr">(12 C, 83 P)</span>
// *              </div>
// *              <div class="CategoryTreeChildren" style="display:none"></div>
// *             </div>
// *
// *    owl-api-subclass
// *    owlsubclassof(child,parent)
// */
//
//
//public class Hierarchy {
//
//    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
//    static String url_prefix = "https://en.wikipedia.org";
//
//    String rootURL = "https://en.wikipedia.org/wiki/Category:Main_topic_classifications";
//    String testRootURL = "https://en.wikipedia.org/wiki/Category:Latin_stubs";
//
//    private IRI ontologyIRI = IRI.create("http://www.daselab.com/wikipedia/v1");
//    private String entityBaseIRI = "http://www.daselab.com/wikipedia/v1#";
//    private String pathToSave = "/Users/sarker/Workspaces/Jetbrains/residue/data/KGS/wikipedia_hierarchy_v9.owl";
//
//    private OWLOntology owlOntology;
//    private OWLDataFactory owlDataFactory;
//    private OWLOntologyManager owlOntologyManager;
//    String categoryIdentifier = "CategoryTreeSection";
//    String categoryHierarchyIdentifier = "CategoryTreeBullet";
//    String getCategoryPegeIdentifier = "CategoryTreeEmptyBullet";
//    String pageIdentifier = "mw-pages";
//    String pagesIdentifier = "mw-category-group";
//    static long counter = 0;
//
//    static HashMap<String, Integer> pageCountHashMap = new HashMap<>();
//
//
//    public void process_indiv(String url, OWLClass parentConcept, Element elementPagesDiv){
//        // parse the page -- individuals
//        if(null != elementPagesDiv){
//            Elements elementsPages = elementPagesDiv.getElementsByClass(pagesIdentifier);
//            if(null != elementsPages){
//                for (Element pagesGroup: elementsPages){
//                    Elements pagesList = pagesGroup.getElementsByTag("a");
//                    if(null != pagesList){
//                        for(Element pageElement: pagesList){
//
//                            // need to exclude template:pageName
//                            String indivName = pageElement.text();
//                            if(indivName.startsWith("Template:")){
//                                // exclude these
//                                logger.info("\tExcluding "+indivName);
//                            }else{
//                                indivName = indivName.trim().replaceAll("[^a-zA-Z0-9]+","_");
//                                logger.info("\tindivName: "+indivName);
//
//                                OWLIndividual owlIndividual = owlDataFactory.getOWLNamedIndividual(IRI.create(entityBaseIRI+indivName));
//                                OWLAxiom indivAddAxiom = owlDataFactory.getOWLClassAssertionAxiom(parentConcept, owlIndividual);
//                                owlOntologyManager.addAxiom(owlOntology, indivAddAxiom);
//                            }
//                        }
//                    }
//                }
//            }
//        }else {
//            logger.info("no page exist for url: " + url);
////                System.out.println("no page exist for url: " + url);
//        }
//    }
//
//    /**
//     * @param parentOwlClass
//     * @param categoryElement
//     * @return
//     */
//
//    public OWLClass process_concept(OWLClass parentOwlClass, Element categoryElement, int level ){
//
//        String levelPrefixString = "";
//        for(int i=0;i<level; i++){
//            levelPrefixString += "  ";
//        }
//
//        String categoryName = categoryElement.getElementsByTag("a").text().trim().replaceAll("[^a-zA-Z0-9]+","_");
//
//        logger.info(levelPrefixString+ "CategoryName: "+categoryName);
//        OWLClass childOwlClass = owlDataFactory.getOWLClass(IRI.create(entityBaseIRI+categoryName));
//        OWLAxiom subClassAxiom = owlDataFactory.getOWLSubClassOfAxiom(childOwlClass, parentOwlClass);
//        owlOntologyManager.addAxiom(owlOntology, subClassAxiom);
//
//        return  childOwlClass;
//    }
//
//    public void process_category(String url, OWLClass parentOwlClass, Elements elementsCategory, int level){
//
//        String levelPrefixString = "";
//        for(int i=0;i<level; i++){
//            levelPrefixString += "  ";
//        }
//
//        // some page will not have category, so the recursive call will stop automatically at those points.
//        // parse the category -- concepts
//        if(null != elementsCategory){
//            for(Element categoryElement : elementsCategory){
//
//                OWLClass owlClass = process_concept(parentOwlClass, categoryElement, level);
//
//                String urlSuffix = categoryElement.getElementsByTag("a").attr("href");
//                String fullURL = url_prefix + urlSuffix;
//
//                if(categoryElement.getElementsByClass(categoryHierarchyIdentifier).isEmpty()){
//                      // no more category hierarchy exist, but page of this category exist.
//                    logger.info(levelPrefixString + urlSuffix+ " don't have more category hierarchy, not visiting.");
//                }else{
//                    // have more hierarchy
//                    logger.info(levelPrefixString + urlSuffix + " have more category hierarchy, need to visit those.");
//                    logger.info(levelPrefixString +"recursive call started: with url: "+fullURL);
//                    process_page(fullURL, owlClass,level+1);
//                }
//            }
//        }else {
////                System.out.println("no category exist for url: " + url);
//            logger.info(levelPrefixString +"no category exist for url: " + url);
//        }
//    }
//
//    public void process_page(String url, OWLClass parentOwlClass, int level){
//        try {
//
//            String levelPrefixString = "";
//            for(int i=0;i<level; i++){
//                levelPrefixString += "  ";
//            }
//            counter++;
//            logger.info("\n\n");
//            logger.info(levelPrefixString +"processing : "+ counter +" pages");
//            logger.info(levelPrefixString +"unique page visit : "+ pageCountHashMap.size() +" pages");
//            if(counter % 100 == 0 ) {
//                Utility.saveOntology(owlOntology, pathToSave);
//                System.gc();
//            }
//            logger.info(levelPrefixString+ "url: "+ url);
//            logger.info(levelPrefixString+ "parentOwlClass: "+ parentOwlClass.getIRI().getShortForm());
//
//            // here we are visiting the url, and parsing it's concents and making those subclass of the parent class.
//            // essentially the url and the parentclass are same.
//            // as mentioned by wikipedia, same category are subclass of multiple category, what we can do is,
//            // if a category is already visited, then don't visit that link again and obviously make sure when we visit a link,
//            // 1. that link is subclass of it's parent link and
//            // 2. all the content's links of that page are subclass of that link
//
//            String inputToMap = url;
//            if(pageCountHashMap.containsKey(inputToMap)){
//                pageCountHashMap.put(inputToMap,  pageCountHashMap.get(inputToMap)+1);
//                logger.info(levelPrefixString+ inputToMap + " being visited: "+ pageCountHashMap.get(inputToMap) + " times, but will not visit actually");
//               // System.exit(1);
//            }else {
//                pageCountHashMap.put(inputToMap, 1);
//                logger.info(levelPrefixString+ inputToMap + " being visited: "+ pageCountHashMap.get(inputToMap) + " times");
//
//                Document document = Jsoup.connect(url).get();
//
//                // elementsCategory is a list of all category
//                Elements elementsCategory = document.getElementsByClass(categoryIdentifier);
//
//                // elementPagesDiv is a big div container of all pages.
//                // Element elementPagesDiv = document.getElementById(pageIdentifier);
//
//                // save to localdisk
//                String path= url.replaceAll("https://en.wikipedia.org/wiki/", "").replaceAll(":","-");
//                path = "/Users/sarker/Workspaces/Jetbrains/residue/data/Wikipedia_html/"+path +".html";
//                File f = new File(path);
//                logger.info(levelPrefixString+ "saving to file: "+ f.getAbsolutePath());
//                FileUtils.writeStringToFile(f, document.outerHtml(), "UTF-8");
//                logger.info(levelPrefixString+ "saved to file: "+ f.getAbsolutePath());
//                f = null;
//                document = null;
//
//                // process_indiv(url,parentOwlClass, elementPagesDiv);
//
//                process_category(url, parentOwlClass, elementsCategory, level);
//            }
//
//        }catch (IOException | OWLOntologyStorageException ex){
//            ex.printStackTrace();
//            System.exit(1);
//        }
//    }
//
//    /**
//     * To decrease the recursion depth call process_page() using each category found here
//     */
//    public void process_main_page(String mainURL, int level){
//
//        String levelPrefixString = "";
//        for(int i=0;i<level; i++){
//            levelPrefixString += "  ";
//        }
//
//        try {
//            logger.info("Started with mainURL: " + mainURL);
//
//            Document document = Jsoup.connect(mainURL).get();
//
//            // elementsCategory is a list of all category
//            Elements elementsCategory = document.getElementsByClass(categoryIdentifier);
//
//            // save to localdisk
//            String path= mainURL.replaceAll("https://en.wikipedia.org/wiki/", "").replaceAll(":","-");
//            path = "/Users/sarker/Workspaces/Jetbrains/residue/data/Wikipedia_html/"+path +".html";
//            File f = new File(path);
//            logger.info("saving to file: "+ f.getAbsolutePath());
//            FileUtils.writeStringToFile(f, document.outerHtml(), "UTF-8");
//            logger.info("saved to file: "+ f.getAbsolutePath());
//            f = null;
//            document = null;
//
//            if(null != elementsCategory){
//                for(Element categoryElement : elementsCategory){
//
//                    // this is a concept
//                    OWLClass owlClass = process_concept(owlDataFactory.getOWLThing(), categoryElement, level);
//
//                    String urlSuffix = categoryElement.getElementsByTag("a").attr("href");
//                    String fullURL = url_prefix + urlSuffix;
//
//                    if(categoryElement.getElementsByClass(categoryHierarchyIdentifier).isEmpty()){
//                        // no more category hierarchy exist, but page of this category exist.
//                        logger.info(levelPrefixString + urlSuffix+ " don't have more category hierarchy, not visiting.");
//                    }else {
//                        // have more hierarchy
//                        logger.info(levelPrefixString + urlSuffix + " have more category hierarchy, need to visit those.");
//                        logger.info(levelPrefixString +"initial call started: with url: "+fullURL);
//                        process_page(fullURL, owlClass,level);
//                    }
//                }
//            }
//        }catch (Exception ex){
//            ex.printStackTrace();
//            System.exit(1);
//        }
//
//    }
//
//
//    public void start_ops() throws OWLOntologyCreationException, OWLOntologyStorageException {
//        owlOntologyManager = OWLManager.createOWLOntologyManager();
//        owlDataFactory = owlOntologyManager.getOWLDataFactory();
//        owlOntology = owlOntologyManager.createOntology(ontologyIRI);
//
//
//        logger.info("process started with url: "+ rootURL);
//        process_main_page(rootURL,0);
//        logger.info("process finished, saving to "+ pathToSave);
//        Utility.saveOntology(owlOntology, pathToSave);
//        logger.info("ontology saved to "+ pathToSave);
//    }
//
//    public static void main(String[]args) throws IOException {
//
//        PropertyConfigurator.configure("/Users/sarker/Workspaces/Jetbrains/residue/java/residue_java_v1/src/main/resources/log4j.properties");
//
//        try {
//            Hierarchy hierarchy = new Hierarchy();
//            hierarchy.start_ops();
////            hierarchy.test();
//        }catch (Exception ex){
//            ex.printStackTrace();
//            System.exit(1);
//        }
//
//    }
//
//    public  void test() throws OWLOntologyCreationException {
//        String url ="https://en.wikipedia.org/wiki/Category:American_Egyptologists";
//
//        owlOntologyManager = OWLManager.createOWLOntologyManager();
//        owlDataFactory = owlOntologyManager.getOWLDataFactory();
//        owlOntology = owlOntologyManager.createOntology(ontologyIRI);
//
//       OWLAxiom axiom = owlDataFactory.getOWLSubClassOfAxiom(owlDataFactory.getOWLClass(IRI.create("#A")), owlDataFactory.getOWLClass(IRI.create("#B")));
//       owlOntologyManager.addAxiom(owlOntology, axiom);
//
//
//       owlOntology.getAxioms().forEach(owlAxiom -> {
//           System.out.println(owlAxiom);
//       });
////           process_page(url,owlDataFactory.getOWLThing());
//    }
//
//    public static void testRun() throws IOException {
//        //        Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/Category:Main_topic_classifications").get();
//        Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/Category:Academic_disciplines").get();
//        // base concepts
//        // exclude the first one as it seems to include the topics again.
//        // so we will have 38 total base concepts.
//        Elements elementsCategory = doc.getElementsByClass("CategoryTreeSection");
//        Element elementPage = doc.getElementById("mw-pages");
//
//        for(Element element : elementsCategory){
//            if(!element.getElementsByClass("CategoryTreeBullet").isEmpty()) {
//
//                // concept name
//                System.out.println(element.getElementsByTag("a").text());
//
//                // link to expand
//                System.out.println(element.getElementsByTag("a").attr("href"));
//                // this would be subclass of the concept name
//                // need to expand it also.
//
//                if (element.getElementsByTag("a").text().equals("Academic disciplines")) {
//                    String url = url_prefix + element.getElementsByTag("a").attr("href");
//
//                    Document doc1 = Jsoup.connect(url).get();
//                    System.out.println("url: "+ url);
////                    System.out.println(doc1);
//
//                    Elements elements1 = doc1.getElementsByClass("CategoryTreeSection");
//
//                    for (Element element1 : elements1) {
//                        if (!element1.getElementsByClass("CategoryTreeBullet").isEmpty()) {
//
//                            // concept name
//                            System.out.println("1" + element1.getElementsByTag("a").text());
//
//                            // link to expand
//                            System.out.println("1" + element1.getElementsByTag("a").attr("href"));
//                            // this would be subclass of the concept name
//                            // need to expand it also.
//                        } else {
//                            System.out.println("empty1");
//                        }
//                    }
//                }
//            }
//            else {
//                System.out.println("empty");
//            }
//        }
//        if(null != elementPage){
//            System.out.println("some text");
//            String pageClass = "mw-category-group";
//            Elements elements2 = elementPage.getElementsByClass(pageClass);
//            if(null != elements2){
//                for (Element element: elements2){
//                    Elements pageElements = element.getElementsByTag("a");
//                    if(null != pageElements){
//                        for(Element pageElement: pageElements){
//                            System.out.println("each page: "+ pageElement.text());
//                        }
//                    }
//                }
//            }
//        }else {
//            System.out.println("nothing");
//        }
//
////        System.out.println(elementsCategory.get(0).getElementsByClass("CategoryTreeBullet")+"  empty");
////        System.out.println(elementsCategory.get(1).getElementsByClass("CategoryTreeBullet"));
//    }
//}
