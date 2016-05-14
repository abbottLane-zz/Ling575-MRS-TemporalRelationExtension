package org.cleartk.timeml.uw.feature.semantics;
import java.util.ArrayList;
import java.util.List;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;
import org.cleartk.syntax.constituent.type.TreebankNode;
import org.cleartk.syntax.constituent.type.TreebankNodeUtil;
/**
 * Created by wlane on 5/12/16.
 */
public class ExplicitTemporalModifiersExtractor<T extends Annotation> implements NamedFeatureExtractor1<T> {
    private String featureName = "ExplicitTemporalModifiers";

    @Override
    public String getFeatureName() {
        return this.featureName;
    }
    @Override
    public List<Feature> extract(JCas view, T focusAnnotation) {
        //System.out.println("extract ExplicitTemporalModifiers has been called");

        List<Feature> features = new ArrayList<Feature>();
        System.out.println(focusAnnotation.getCoveredText() + " at indexes: " + focusAnnotation.getBegin() + " to " + focusAnnotation.getEnd());
        System.out.println(focusAnnotation.toString());
        features.add(new Feature(this.featureName, focusAnnotation.getCoveredText()));

        // Break down full document text into sentences
        String[] sents = view.getDocumentText().split("\n");
        List<String> clean_sents = new ArrayList<String>();
        for (String sent : sents){
            if(!sent.equals("") && !sent.equals("\n"))
            {
                clean_sents.add(sent);
            }
        }

        System.out.println(clean_sents.toString());
        return features;
    }
}

