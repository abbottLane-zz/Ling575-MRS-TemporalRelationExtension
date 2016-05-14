package org.cleartk.timeml.uw.feature.semantics;

/**
 * Created by wlane on 5/14/16.
 */
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.FeatureExtractor2;
import org.cleartk.syntax.constituent.type.TreebankNode;
import org.cleartk.syntax.constituent.type.TreebankNodeUtil;
import org.cleartk.syntax.constituent.type.TreebankNodeUtil.TreebankNodePath;

import com.google.common.base.Joiner;
/**
 * Created by wlane on 5/12/16.
 */
public class SemanticPathBetweenEventsExtractor<T extends Annotation, U extends Annotation> implements FeatureExtractor2<T, U> {
    public List<Feature> extract(JCas jCas, T source, U target) {

        System.out.println("Oi m8, extractin' sum featchas 'tween events");
        List<Feature> features = new ArrayList<Feature>();
        TreebankNode sourceNode = TreebankNodeUtil.selectMatchingLeaf(jCas, source);
        TreebankNode targetNode = TreebankNodeUtil.selectMatchingLeaf(jCas, target);
        if (sourceNode != null && targetNode != null) {
            TreebankNodePath path = TreebankNodeUtil.getPath(sourceNode, targetNode);
            TreebankNode ancestor = path.getCommonAncestor();
            features.add(new Feature("CommonAncestor", ancestor == null ? null : ancestor.getNodeType()));
            features.add(new Feature("SourceToAncestor", pathString(path.getSourceToAncestorPath())));
            features.add(new Feature("TargetToAncestor", pathString(path.getTargetToAncestorPath())));
        }
        return features;
    }

    private static String pathString(List<TreebankNode> nodes) {
        // strip the first node from the list
        nodes = nodes.subList(Math.min(1, nodes.size()), nodes.size());

        // join the types with underscores
        List<String> types = new ArrayList<String>();
        for (TreebankNode node : nodes) {
            types.add(node.getNodeType());
        }
        return Joiner.on('_').join(types);
    }

}

