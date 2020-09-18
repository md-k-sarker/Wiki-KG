package Util;


import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SharedDataHolder {

    public static String programStartingDir;
    public static Map<String, String> prefixmap = new HashMap<>();
    public static OWLDocumentFormat owlDocumentFormat;

    public static final String noneObjPropStr = "__%!empty%!__";
    public static final OWLObjectProperty noneOWLObjProp = OWLManager.getOWLDataFactory().getOWLObjectProperty(IRI.create(noneObjPropStr));


}
