package org.cleartk.timeml.uw.feature.semantics;

/**
 * Created by wlane on 5/14/16.
 */
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.sun.org.apache.xml.internal.serializer.utils.SystemIDResolver;
import com.sun.org.apache.xpath.internal.operations.Bool;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import opennlp.tools.tokenize.Detokenizer;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.FeatureExtractor2;
import org.cleartk.syntax.constituent.type.TreebankNode;
import org.cleartk.syntax.constituent.type.TreebankNodeUtil;
import org.cleartk.syntax.constituent.type.TreebankNodeUtil.TreebankNodePath;

import java.io.IOException;
import java.lang.ProcessBuilder;
import java.util.*;

import opennlp.tools.tokenize.DictionaryDetokenizer;
import opennlp.tools.tokenize.DetokenizationDictionary;

import com.google.common.base.Joiner;
/**
 * Created by wlane on 5/12/16.
 */
public class SemanticPathBetweenEventsExtractor<T extends Annotation, U extends Annotation> implements FeatureExtractor2<T, U> {
    public List<Feature> extract(JCas jCas, T source, U target) {

        //Derive the sentence from which the events come
        TreebankNode sourceNode = TreebankNodeUtil.selectMatchingLeaf(jCas, source);
        TreebankNode topNode = TreebankNodeUtil.getTopNode(sourceNode);
        String sentenceParseString = TreebankNodeUtil.toTreebankString(topNode);
        PennTreeReader sentTree = new PennTreeReader(new StringReader(sentenceParseString), new LabeledScoredTreeFactory());

        // Reform the original sentence from the PTB tree
        String originalSentence = "";
        try {
            List<Tree> toks = sentTree.readTree().getLeaves();
            originalSentence = getOriginalSentence(toks);
            originalSentence = originalSentence.replace("\"", ""); //quotes mess up the module that speaks to python
            originalSentence = originalSentence.replace("\'\'", "");
            originalSentence = originalSentence.replace("_", "");
            originalSentence = originalSentence.replace("``", "");
            originalSentence = originalSentence.replace("-RRB-", "");
            originalSentence = originalSentence.replace("dlrs", "$");
            originalSentence = originalSentence.replace("  ", " ");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Append every sentence to a file
        String e1 = source.getCoveredText();
        String e2 = target.getCoveredText();
        int e1_begin = originalSentence.indexOf(e1);
        int e1_end = originalSentence.indexOf(e1) + e1.length();
        int e2_begin = originalSentence.indexOf(e2);
        int e2_end = originalSentence.indexOf(e2)+e2.length();

        // in cache-prep mode, write all sentences to a file
        String cached_sent_filename = "/home/wlane/IdeaProjects/Ling575-MRS-TemporalRelationExtension/src/main/resources/CachedData/cachedSentences.dev.FULL.out";
        cacheSentences(cached_sent_filename, e1,e1_begin,e1_end,e2,e2_begin,e2_end,originalSentence);

        // for current sentence, read in the cached feature set using a key
        StringBuilder key = new StringBuilder();
        key.append(e1);
        key.append(e1_begin);
        key.append(e1_end);
        key.append(e2);
        key.append(e2_begin);
        key.append(e2_end);
        String key_str = key.toString();

        String cachedFeatureDictDir = "src/main/resources/CachedData/cachedFeatureDictionary.dev.out";

//        // Create python cmd line command
//        String line = "python3 /home/wlane/IdeaProjects/Ling575-MRS-TemporalRelationExtension/src/main/resources/PythonScripts/feature_extractor.py "+ e1 + " " + e1_begin + " " +e1_end + " " + e2+ " " + e2_begin + " " + e2_end + " \""+ originalSentence + "\"";
//
////        line = "/home/wlane/IdeaProjects/relation_extractor/src/main/resources/ace/ace  -g /home/wlane/IdeaProjects/relation_extractor/src/main/resources/ace/erg-1214-x86-64-0.9.22.dat -1Tf \n" + originalSentence+" \n";
//        System.out.println("JAVA:" + line);
//        CommandLine cmdLine = CommandLine.parse(line);
//        String pythonReply = "no response from python script";
//
//        //Catch the output of the python script
//        try {
//            pythonReply = execToString(line);
//
//            System.out.println("PYTH: " + pythonReply);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        //convert python script output to Feature objects, only if we got back valid features
//        if(pythonReply.startsWith("e1") || pythonReply.startsWith("e2")) {
//            String[] feats = pythonReply.split("\\s+");
//            for (String feature : feats) {
//                String[] name_val = feature.split("=");
//                if (name_val[0].contains("/usr/") || name_val[1].contains("/usr/")){
//                    break;
//                }
//                features.add(new Feature(name_val[0], name_val[1]));
//                //System.out.println("NAME:" + name_val[0] + " " + "VAL:" + name_val[1] + " ");
//            }
//        }
        return  getFeatureList(key_str, cachedFeatureDictDir);
    }

    private List<Feature> getFeatureList(String key_str, String cachedFeatureDictDir) {

        // First load the dictionary
        //HashMap<String, String> featureDict = new HashMap<String, String>();
        String featurs_str = "";
        try (BufferedReader br = new BufferedReader(new FileReader(cachedFeatureDictDir))) {
            String line;
            Boolean key_found = false;
            while ((line = br.readLine()) != null) {
                // process the line.
                String[] key_val = line.split("=:=");
                if(key_val[0].equals(key_str)){
                    featurs_str = key_val[1];
                    key_found = true;
                }
            }
            if(!key_found)
            {
                System.out.println("KEY NOT FOUND !!!: " + key_str);
                System.out.println();
                List<Feature> not_found = new ArrayList<>();
                not_found.add(new Feature("NOT_FOUND", "NOT_FOUND"));
                return not_found;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] feat_toks = featurs_str.split(" ");

        // create Feature objects
        List<Feature> features = new ArrayList<>();
        for (String feature : feat_toks) {
            if(!feature.equals("NO_PARSE") && !feature.equals("NO_FEATS")) {
                String[] name_val = feature.split("=");
                if (name_val[0].contains("/usr/") || name_val[1].contains("/usr/")) {
                    break;
                }
                features.add(new Feature(name_val[0], name_val[1]));
                //System.out.println("NAME:" + name_val[0] + " " + "VAL:" + name_val[1] + " ");
            }
        }
        return features;
    }

    private void cacheSentences(String cached_sent_filename, String e1, int e1_begin, int e1_end, String e2, int e2_begin, int e2_end, String originalSentence) {
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(cached_sent_filename, true));
            StringBuilder sb = new StringBuilder();
            sb.append(e1);
            sb.append("#-#");
            sb.append(e1_begin);
            sb.append("#-#");
            sb.append(e1_end);
            sb.append("#-#");
            sb.append(e2);
            sb.append("#-#");
            sb.append(e2_begin);
            sb.append("#-#");
            sb.append(e2_end);
            sb.append("#-#");
            sb.append(originalSentence);
            sb.append("\n");
            output.append(sb.toString());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String execToString(String command) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CommandLine commandline = CommandLine.parse(command);
        DefaultExecutor exec = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(30*1000);
        exec.setWatchdog(watchdog);
        exec.setExitValue(0);
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);
        exec.execute(commandline);
        return(outputStream.toString());
    }
    private String getOriginalSentence(List<Tree> toks) throws IOException {

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < toks.size(); i++){

            sb.append(toks.get(i).toString());
            if(i+1 < toks.size()){
                if(!toks.get(i+1).toString().equals(",")
                        && !toks.get(i).toString().equals("$")
                        && !toks.get(i+1).toString().equals("'")
                        && !toks.get(i+1).toString().equals(".")
                        && !toks.get(i+1).toString().equals("n\'t")
                        && !toks.get(i+1).toString().equals("\'s")
                        && !toks.get(i+1).toString().equals("\'ll")
                        && !toks.get(i+1).toString().equals("\'m")
                        && !toks.get(i+1).toString().equals("\'re")
                        && !toks.get(i+1).toString().equals("%")
                        )
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}

