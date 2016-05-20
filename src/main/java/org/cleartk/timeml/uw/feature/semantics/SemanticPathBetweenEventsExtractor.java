package org.cleartk.timeml.uw.feature.semantics;

/**
 * Created by wlane on 5/14/16.
 */
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
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

import com.google.common.base.Joiner;
/**
 * Created by wlane on 5/12/16.
 */
public class SemanticPathBetweenEventsExtractor<T extends Annotation, U extends Annotation> implements FeatureExtractor2<T, U> {
    public List<Feature> extract(JCas jCas, T source, U target) {

        List<Feature> features = new ArrayList<Feature>();

        //Derive the sentance from which the events come
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Pass event and sentence info to python script
        String e1 = source.getCoveredText();
        String e2 = target.getCoveredText();
        int e1_begin = source.getBegin();
        int e1_end = source.getEnd();
        int e2_begin = target.getBegin();
        int e2_end = target.getEnd();
        String line = "python3 ./src/main/resources/PythonScripts/get_ace_features.py "+ e1 + " " + e1_begin + " " +e1_end + " " + e2+ " " + e2_begin + " " + e2_end + " \""+ originalSentence + "\"";
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        String pythonReply = "no response from python script";

        //Catch the output of the python script
        try {
            pythonReply = execToString(line);
            System.out.println("PYTH: " + pythonReply);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //convert python script output to Feature objects
        return features;
    }
    public String execToString(String command) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CommandLine commandline = CommandLine.parse(command);
        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);
        exec.execute(commandline);
        return(outputStream.toString());
    }
    private String getOriginalSentence(List<Tree> toks) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < toks.size(); i++){

            sb.append(toks.get(i).toString());
            if(i+1 < toks.size()){
                if(!toks.get(i+1).toString().equals(",") && !toks.get(i+1).toString().equals("'") && !toks.get(i+1).toString().equals(".") && !toks.get(i+1).toString().equals("n\'t") && !toks.get(i+1).toString().equals("\'s"))
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}

