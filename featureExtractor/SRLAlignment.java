package edu.oregonstate.featureExtractor;

import java.util.*;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.server.ResultAggregation;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;

/**
 * align the SRL results with the corresponding mentions of the specific document
 * 
 * Alignment Principle : It is often the case that the semantic arguments output 
 * by the SRL parser do not align with any of the previously identified noun phrases.
 * In this case, we pass a semantic role label to a RE only when the two phrases share the 
 * same head. from the paper called Exploiting Semantic Role Labeling, WordNet and Wikipedia for 
 * Coreference Resolution
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SRLAlignment {

	/** the index of each column is obtained by decrementing one from the original column number */
	protected static final int WORD_NO = 0;
	protected static final int WORD_FORM = 1;
	protected static final int WORD_HEAD = 8;
	protected static final int PRED = 10;

	/** result path */
	private final String mSRLResultPath;

	/** SRL document */
	private final SRLDocument document;

	/** fixed roles considered in our experiment */
	private final List<String> ROLES = Arrays.asList(new String[]{"A0", "A1", "A2", "AM-LOC"});

	public SRLAlignment(String srlResultPath) {
		mSRLResultPath = srlResultPath;

		// read the SRL document
		SRLDocumentReader reader = new SRLDocumentReader(mSRLResultPath);
		document = reader.readDocument();
	}

	/**
	 * align SRL result with the predicted mentions
	 * 
	 * @param predictedOrderedMentionsBySentence
	 */
	public void alignMentions(List<List<Mention>> predictedOrderedMentionsBySentence) {
		int noOfSentence = predictedOrderedMentionsBySentence.size();
		int mentionIDOffset = 0;
		RuleBasedCorefMentionFinder headFinder = new RuleBasedCorefMentionFinder();
		String sentenceAlignmentDebugInformation = document.getDocumentID() + " : " + document.getSentences().size() + "  " + noOfSentence; 
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, sentenceAlignmentDebugInformation);

		// for each sentence
		for (int index = 0; index < noOfSentence; index++) {
			List<Mention> mentions = predictedOrderedMentionsBySentence.get(index);
			

			// find the global annotation
			List<CoreLabel> sentenceWords = mentions.get(0).sentenceWords;
			int sentenceLength = sentenceWords.size();
			
			SemanticGraph dependency = mentions.get(0).dependency;
			Tree tree = mentions.get(0).contextParseTree;
			tree.indexSpans(0);

			Map<Span, Mention> detectedMentionSpan = new HashMap<Span, Mention>();
			
			// one head index may correspond to several mentions
			Map<Integer, LinkedList<Mention>> detectedMentionHeadSpan = new HashMap<Integer, LinkedList<Mention>>();
			// put the mention span into a map
			for (Mention mention : mentions) {
				int mentionStartIndex = mention.startIndex;
				int mentionEndIndex = mention.endIndex;
				Span mentionSpan = new Span(mentionStartIndex, mentionEndIndex);
				detectedMentionSpan.put(mentionSpan, mention);

				int mentionHeadIndex = mention.headIndex;
				if (!detectedMentionHeadSpan.containsKey(mentionHeadIndex)) {
					detectedMentionHeadSpan.put(mentionHeadIndex, new LinkedList<Mention>());
				}
				
				detectedMentionHeadSpan.get(mentionHeadIndex).add(mention);
			}

			// SRL annotation for that sentence
			List<String[]> sentence = document.getSentences().get(index);
			int length = sentence.size();
			// get the dependency relationship between each token
			int[] parents = new int[length];
			for (int row = 0; row < length; row++) {
				int parent = Integer.parseInt(sentence.get(row)[WORD_HEAD]);  // parse the head index string to head index integer
				parents[row] = parent - 1;
			}
			// build the syntactic dependency tree
			Map<Integer, Set<Integer>> graph = buildGraph(parents);

			// find predicate and its corresponding argument column, when find a predicate and its arguments,
			// align with detected mentions
			int predOffset = 0;
			for (int row = 0; row < length; row++) {
				String[] token = sentence.get(row);
				String predicate = token[PRED];
				if (!predicate.equals("_")) {
					// find the predIndex and construct the Span
					// two choices: align just using mention span and also using the mention head index
					int predIndex = row;
					Span predicateSpan = new Span(predIndex, predIndex + 1);
					boolean mentionContainPred = detectedMentionSpan.containsKey(predicateSpan);
					Mention predMention = null;
					if (mentionContainPred) {
						predMention = detectedMentionSpan.get(predicateSpan);
					}
					// use the mention head index
					//					else {
					//						boolean mentionHeadContainPred = detectedMentionHeadSpan.containsKey(predIndex);
					//						if (mentionHeadContainPred) {
					//							predMention = detectedMentionHeadSpan.get(predIndex);
					//						}
					//					}

					// increment the offset
					predOffset += 1;

					// if can not find detected mention for the predicate, then move forward to 
					// next predicate
					if (predMention == null) {
						continue;
					}

					// align with the arguments
					int argumentColumn = PRED + predOffset;
					// find the arguments
					String[] annotations = getColumn(argumentColumn, sentence);
					for (int argumentRow = 0; argumentRow < length; argumentRow++) {
						String annotation = annotations[argumentRow];
						// focus on four roles : A0, A1, A2, AM-LOC
						if (!annotation.equals("_") && ROLES.contains(annotation)) {
							List<Integer> argumentYieldSpan = yield(argumentRow, graph);
							
							int argumentStartIndex = argumentYieldSpan.get(0);
							int argumentEndIndex = argumentYieldSpan.get(argumentYieldSpan.size() - 1) + 1;
							Span argumentSpan = new Span(argumentStartIndex, argumentEndIndex);
							Mention argumentMention = null;
							boolean mentionContainArgument = detectedMentionSpan.containsKey(argumentSpan);
							// match the mention boundary
							if (mentionContainArgument) {
								argumentMention = detectedMentionSpan.get(argumentSpan);								
								predMention.addArgument(annotation, argumentMention);
								argumentMention.addPredicate(predMention, annotation);
							} else {
								// construct an mention
								if (argumentStartIndex < 0) {
									argumentStartIndex = 0;
								}
								if (argumentEndIndex > sentenceLength) {
									argumentEndIndex = sentenceLength;
								}
								
								try {
									Mention argumentMentionForHeadFinding = new Mention(mentionIDOffset, argumentStartIndex, argumentEndIndex, 
											dependency, new ArrayList<CoreLabel>(sentenceWords.subList(argumentStartIndex, argumentEndIndex)));

									Tree head = headFinder.findSyntacticHead(argumentMentionForHeadFinding, tree, sentenceWords);

									int headIndex = ((CoreLabel) head.label()).get(IndexAnnotation.class)-1;
									boolean mentionHeadContainArgument = detectedMentionHeadSpan.containsKey(headIndex);
									if (mentionHeadContainArgument) {
										argumentMention = detectedMentionHeadSpan.get(headIndex).poll();
										// only noun phrase can be the argument of the predicate
										if ((argumentMention != null) && (!argumentMention.isVerb)) {
											predMention.addArgument(annotation, argumentMention);
											argumentMention.addPredicate(predMention, annotation);
										}
									}
								} catch (Exception e) {
									System.out.println(predMention.toString() + " " + index + " " + argumentStartIndex + " " + argumentEndIndex + " " + sentenceLength);
									throw new RuntimeException(e);
								}

								mentionIDOffset += 1;
							}

						}
					}

				}

			}

		}

	}

	/**
	 * get the column annotation from the sentence
	 * 
	 * @param columnIndex
	 * @param sentence
	 * @return
	 */
	private String[] getColumn(int columnIndex, List<String[]> sentence) {
		int length = sentence.size();
		String[] column = new String[length];
		for (int row = 0; row < length; row++) {
			String[] token = sentence.get(row);
			String annotation = token[columnIndex];
			column[row] = annotation;
		}
		return column;
	}

	/**
	 * use breadth search to find all yields of the parent
	 * 
	 * @param row
	 * @param graph
	 * @return a sorted list
	 */
	private List<Integer> yield(int row, Map<Integer, Set<Integer>> graph) {
		List<Integer> span = new ArrayList<Integer>();
		span.add(row);
		Queue<Integer> queue = new ArrayDeque<Integer>();
		queue.offer(row);
		// do breadth first search
		while (!queue.isEmpty()) {
			int parent = queue.poll();
			Set<Integer> children = graph.get(parent);
			if (children == null) continue;

			for (Integer child : children) {
				queue.offer(child);
				span.add(child);
			}
		}

		// sort the span
		Collections.sort(span);

		return span;
	}


	/**
	 * build a syntactic dependency tree 
	 *  
	 * @param parents : key is the parent, Set<Integer> is the children of the parent
	 * @return
	 */
	private Map<Integer, Set<Integer>> buildGraph(int[] parents) {
		Map<Integer, Set<Integer>> graph = new HashMap<Integer, Set<Integer>>();

		for (int index = 0; index < parents.length; index++) {
			int parent = parents[index];
			boolean contain = graph.containsKey(parent);
			if (!contain) {
				graph.put(parent, new HashSet<Integer>());
			}

			graph.get(parent).add(index);
		}

		return graph;
	}

}
