/*
 * Copyright (c) 2011, Regents of the University of Colorado
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.cleartk.timeml.tlink;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.FeatureExtractor2;
import org.cleartk.ml.feature.extractor.TypePathExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Bag;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.syntax.constituent.type.TreebankNode;
import org.cleartk.syntax.constituent.type.TreebankNodeUtil;
import org.cleartk.timeml.type.Anchor;
import org.cleartk.timeml.type.Event;
import org.cleartk.timeml.util.CleartkInternalModelFactory;
import org.cleartk.feature.syntax.SyntacticFirstChildOfGrandparentOfLeafExtractor;
import org.cleartk.feature.syntax.SyntacticLeafToLeafPathPartsExtractor;
import org.cleartk.timeml.uw.feature.semantics.ExplicitTemporalModifiersExtractor;
import org.cleartk.timeml.uw.feature.semantics.SemanticPathBetweenEventsExtractor;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;

/**
 * <br>
 * Copyright (c) 2011, Regents of the University of Colorado <br>
 * All rights reserved.
 *
 * @author Steven Bethard
 */
public class OurTemporalLinkEventToSubordinatedEventAnnotator extends
        TemporalLinkAnnotator_ImplBase<Event, Event> {

    public static final CleartkInternalModelFactory FACTORY = new CleartkInternalModelFactory() {
        @Override
        public Class<?> getAnnotatorClass() {
            return TemporalLinkEventToSubordinatedEventAnnotator.class;
        }

        @Override
        public Class<?> getDataWriterClass() {
            return LibLinearStringOutcomeDataWriter.class;
        }

        @Override
        public AnalysisEngineDescription getBaseDescription() throws ResourceInitializationException {
            return AnalysisEngineFactory.createEngineDescription(TemporalLinkEventToSubordinatedEventAnnotator.class);
        }
    };

    public OurTemporalLinkEventToSubordinatedEventAnnotator() {
        super(Event.class, Event.class, "BEFORE", "AFTER");
    }

    private static final Pattern SUBORDINATE_PATH_PATTERN = Pattern.compile("^(VP>|ADJP>|NP>)?(VP|ADJP|S|SBAR)(<(S|SBAR|PP))*((<VP|<ADJP)*|(<NP)*)$");

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        System.out.println("We are extracting features now ...");
        List<FeatureExtractor1<Event>> extractors = Lists.newArrayList();
        extractors.add(new TypePathExtractor<Event>(Event.class, "tense"));
        extractors.add(new TypePathExtractor<Event>(Event.class, "aspect"));
        extractors.add(new TypePathExtractor<Event>(Event.class, "eventClass"));
        extractors.add(new SyntacticFirstChildOfGrandparentOfLeafExtractor<Event>());
        //TEST: extractors.add(new ExplicitTemporalModifiersExtractor<Event>());



        this.setSourceExtractors(extractors);
        this.setTargetExtractors(extractors);

        List<FeatureExtractor2<Anchor, Anchor>>btweenExtractors = Lists.newArrayList();
        btweenExtractors.add(new SyntacticLeafToLeafPathPartsExtractor<Anchor, Anchor>());
        btweenExtractors.add(new SemanticPathBetweenEventsExtractor<Anchor, Anchor>());
        btweenExtractors.add(new CleartkExtractor<Anchor, Token>(Token.class, new CoveredTextExtractor<Token>(), new Bag(new Covered())));
        this.setBetweenExtractors(btweenExtractors);
    }

    @Override
    protected List<SourceTargetPair> getSourceTargetPairs(JCas jCas) {
        List<SourceTargetPair> pairs = Lists.newArrayList();
        for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
            for (Event source : JCasUtil.selectCovered(jCas, Event.class, sentence)) {
                for (Event target : this.getSubordinateEvents(jCas, source, sentence)) {
                    pairs.add(new SourceTargetPair(source, target));
                }
            }
        }
        return pairs;
    }

    private List<Event> getSubordinateEvents(JCas jCas, Event source, Sentence sentence) {
        List<Event> targets = new ArrayList<Event>();
        TreebankNode sourceNode = TreebankNodeUtil.selectMatchingLeaf(jCas, source);
        for (Event target : JCasUtil.selectCovered(jCas, Event.class, sentence)) {
            if (!target.equals(source)) {
                TreebankNode targetNode = TreebankNodeUtil.selectMatchingLeaf(jCas, target);
                if (sourceNode != null && targetNode != null) {
                    String path = noLeavesPath(TreebankNodeUtil.getPath(sourceNode, targetNode));
                    if (SUBORDINATE_PATH_PATTERN.matcher(path).matches()) {
                        targets.add(target);
                    }
                }
            }
        }
        return targets;
    }
}

