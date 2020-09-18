package wiki.creation;

import exceptions.SameInputOutputPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Fix the unicode problem of ontology.
 * Specifically, remove/replace any non-unicode from the ontology
 */
public class UnicodeProblem {
    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String inputKGPath;
    private String outputKGPath;


    /**
     * Constructor
     *
     * @param inputKGPath
     * @param outputKGPath
     */
    public UnicodeProblem(String inputKGPath, String outputKGPath) throws SameInputOutputPathException {
        if (inputKGPath.equals(outputKGPath)) {
            throw new SameInputOutputPathException("InputKGPath " + inputKGPath + " and \n " +
                    "OutputKGPath " + outputKGPath + " must not be same.");
        }
        this.inputKGPath = inputKGPath;
        this.outputKGPath = outputKGPath;
    }

    /**
     * @param replaceBytes
     * @return
     */
    public boolean removeNonUnicode(HashMap<Byte, Character> replaceBytes) {

        long time = System.currentTimeMillis();

        // The input & outputFile
        File inFile = new File(inputKGPath);
        File outputFile = new File(outputKGPath);
        int count = 0;
        // read 1 byte as, reading multiple bytes may give error on the last reading, by raising not enough byte to read.
        byte[] buffer = new byte[1];

        try {
            FileInputStream fis = new FileInputStream(inFile);
            FileOutputStream fos = new FileOutputStream(outputFile);

            while (fis.available() > 0) {
                fis.read(buffer);

                for (Map.Entry<Byte, Character> replaceByte : replaceBytes.entrySet()) {
                    if (buffer[0] == replaceByte.getKey().byteValue()) {
                        count += 1;
                        fos.write(replaceByte.getValue());
                    } else {
                        fos.write(buffer);
                    }
                }
            }

            fis.close();
            fos.close();

        } catch (SecurityException e) {
            logger.error("security manager exists and its checkWrite method denies read/write access to the inFile");
            e.printStackTrace();
            return false;
        } catch (FileNotFoundException e) {
            logger.error("inFile does not exist,\n" +
                    " is a directory rather than a regular inFile,\n" +
                    " or for some other reason cannot be opened for reading/writing.");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            logger.error("I/O error occurred");
            e.printStackTrace();
            return false;
        }

        long delta = System.currentTimeMillis() - time;
        logger.info("Found " + count + " instances of the <0x92> character in " + delta / 1000 + " seconds.");

        return true;
    }


    /**
     * byte windowsBullshit = (new Byte("0x92")).byteValue();
     * byte windowsBullshit = (byte) 0b1001_0010;
     */

    public static void main(String[] args) {
        String inpGraph = "../data/wiki_full_cats_with_pages_v1_non_cyclic_jan_20_32808131.rdf";
        String outpGraph = "../data/wiki_full_cats_with_pages_v1_non_cyclic_jan_20_32808131_fixed_non_unicode.rdf";

        try {
            UnicodeProblem unicodeProblem = new UnicodeProblem(inpGraph, outpGraph);
            HashMap<Byte, Character> replaceMap = new HashMap<>();

            replaceMap.put((byte) 0b1001_0010, '\'');
            unicodeProblem.removeNonUnicode(replaceMap);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void coganTest() {

        String filename = "graph.rdf";
//		String filename = "problems.txt";
        int count = 0;

//		byte windowsBullshit = (new Byte("0x92")).byteValue();
        byte windowsBullshit = (byte) 0b1001_0010;

        Character c = '\'';

        byte[] buffer = new byte[1];
        try {
            /** This Code cannot work for the graph.rdf
             * because you cannot create an array of that size*/
//			byte[] bytes = Files.readAllBytes(file.toPath());
//			for(byte b : bytes)
//			{
//				if (b == windowsBullshit)
//				{
//					count += 1;
//				}
//			}

            // The input
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);

            // The output
            File output = new File("output.rdf");
            FileOutputStream fos = new FileOutputStream(output);


            while (fis.available() > 0) {
                fis.read(buffer);

                if (buffer[0] == windowsBullshit) {
                    count += 1;
                    fos.write(c);
                } else {
                    fos.write(buffer);
                }
            }

            fis.close();
            fos.close();
        } catch (FileNotFoundException e) {
//			System.out.println("Can't find file called " + filename);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Can't close the BR for some reason?");
        }
    }
}