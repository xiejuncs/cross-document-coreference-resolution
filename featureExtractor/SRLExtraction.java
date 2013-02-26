package edu.oregonstate.featureExtractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.oregonstate.data.EecbSrlAnnotation;


/**
 * Interface for Semantic parsing: PropBank--NomBank frames output.
 * The pattern of the output is listed in the CoNLL website : 
 * http://barcelona.research.yahoo.net/dokuwiki/doku.php?id=conll2008:format
 * <p>
 * In addition, there is an test file at http://fileadmin.cs.lth.se/nlp/software/lth_srl/test.txt
 * <b>Example</b>
 * Run the second sentence :
 * With the passing of the years, the Sixties working-class wonder boy has metamorphosed into a very cross pensioner.
 * We can get the output.
 * In short: the predicates recognized by the tool are listed in column 11. In this case, we have 3 predicates:
 *  "passing", "boy", and "metamorphose". For each of these 3 predicates, there is an additional column listing 
 *  the arguments of that predicate. Thus the first column after the predicate column lists the arguments for 
 *  "passing"; in this case we have only one argument, the A1. Note that format lists only the heads of the arguments
 *  , in this case "of". If you want the full argument span "of the years", you'll need to generate the yield of the 
 *  syntactic dependency tree (column 9).
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SRLExtraction {

	private Map<Integer, List<List<String>>> document;
	private Map<Node, List<Node>> nodes;
	private List<Integer> headsPosition;
	private List<Node> outputNode;
	
	/**
	 * initialize the document.
	 * <b>NOTE</b> document can be treated as a list of sentences, which consist of a bunch of words.
	 * The key of the document hashmap structure book-keeps the sentence id;
	 * The output of each token in one sentence can be represented as a List<String>. So each sentence is represented as a List<List<String>> structure.
	 * 
	 * @param path
	 */
	public SRLExtraction() {
		document = new HashMap<Integer, List<List<String>>>();
	}

	public void setDocument(Map<Integer, List<List<String>>> document) {
		this.document = document;
	}
	
	public Map<Integer, List<List<String>>> getDocument() {
		return this.document;
	}
	
	/**
	 * Search data structure
	 * 
	 * @author Jun Xie (xie@eecs.oregonstate.edu)
	 *
	 */
	public class Node
    {
        int data;
        Node predecessor;
        
        public Node(int data)
        {
            this.data = data;
        }
        
        public String toString()
        {
            return "(" + data + ")";
        }
    }
	
	/**
	 * use breadth-first search algorithm to extract the extent of the specific head 
	 * @param s
	 * @return
	 */
    public List<Integer> bfs(Node s) {    	
    	List<Integer> children = new ArrayList<Integer>();
        Set<Node> keys = nodes.keySet();
        for (Node u : keys) {
            if (u != s) {
                u.predecessor = null;
            }
        }
        s.predecessor = null;
        Queue<Node> q = new ArrayDeque<Node>();
        q.add(s);
        while (!q.isEmpty()) {
            Node u = q.remove();
            List<Node> adj_u = nodes.get(u);
            if (adj_u != null) {
                for (Node v : adj_u) {
                    v.predecessor = u;
                    q.add(v);
                }
            }
            children.add(u.data);
        }
        return children;
    }
    
    public void addEdge(Node n1, Node n2)
    {
        if (nodes.containsKey(n1)) {
            nodes.get(n1).add(n2);
        } else {
            ArrayList<Node> list = new ArrayList<Node>();
            list.add(n2);
            nodes.put(n1, list);
        }
    }
    
    public EecbSrlAnnotation find(int headPosition, List<EecbSrlAnnotation> tokens) {
    	EecbSrlAnnotation span = new EecbSrlAnnotation();
		int startIndex = 0;
		int endIndex = 0;
		List<Integer> spans = new ArrayList<Integer>();
		spans = bfs(outputNode.get(headPosition));
		startIndex = Collections.min(spans);
		endIndex = Collections.max(spans);
		assert endIndex > startIndex;
		span.setStartOffset(startIndex);
		span.setEndOffset(endIndex + 1);
		StringBuffer sb = new StringBuffer();
		for (int i = startIndex; i < endIndex + 1; i++) {
			sb.append(tokens.get(i).getText() + " ");
		}
		span.setText(sb.toString().trim());
		return span;
	}
    
    public Map<EecbSrlAnnotation, Map<String, EecbSrlAnnotation>> extractExtent(List<List<String>> sentence) {
    	List<EecbSrlAnnotation> annotations = new ArrayList<EecbSrlAnnotation>();
		for (int i = 0; i < sentence.size(); i++) {
			List<String> data = sentence.get(i);
			
			String token = data.get(1);
			String id = data.get(0);
			int start = Integer.parseInt(id);
			int end = start + 1;
			String predicate = data.get(10);
			String position = data.get(8);
			int pos = 0;
			if (position.equals("0")) {
				pos = i;
			} else {
				pos = Integer.parseInt(position) - 1;
			}
			
			EecbSrlAnnotation anno = new EecbSrlAnnotation(id, token, predicate, pos, start, end);
			annotations.add(anno);
		}
		
		/** find how many predicates annotated by the semantic role labeling software*/
		List<Integer> predicates = new ArrayList<Integer>();
		for (int i = 0; i < annotations.size(); i++) {
			EecbSrlAnnotation annotation = annotations.get(i);
			String pred = annotation.getPredicate();
			if (pred.equals("_"))	continue;
			predicates.add(i);
		}
		
		// find the head position
		int predSize = predicates.size();
		Map<Integer, Map<String, Integer>> arguments = new HashMap<Integer, Map<String,Integer>>();
		for (int i = 11; i <= (10 + predSize); i++) {
			int slot = i - 11;
			int index = predicates.get(slot);
			Map<String, Integer> argument = new HashMap<String, Integer>();
			for (int j = 0; j < sentence.size(); j++) {
				List<String> data = sentence.get(j);
				if (data.get(i).equals("A0") || data.get(i).equals("A1") || data.get(i).equals("A2") || data.get(i).equals("AM-LOC")) {
					argument.put(data.get(i), j);
				}
			}
			arguments.put(index, argument);
		}
		
		headsPosition = new ArrayList<Integer>();
		// find the head dependency output
		for (int i = 0; i < sentence.size(); i++) {
			List<String> data = sentence.get(i);
			String position = data.get(8);
			if (position.equals("0")) {
				headsPosition.add(i);
			} else {
				Integer pos = Integer.parseInt(data.get(8));
				headsPosition.add(pos -1);
			}
		}
		
		outputNode = new ArrayList<Node>();
		nodes = new HashMap<Node, List<Node>>();
		// initialize the arguments for the breadth-first search
		for (int i = 0; i < headsPosition.size(); i++) {
			outputNode.add(new Node(i));
		}
		for (int i = 0; i < headsPosition.size(); i++) {
			int parent = headsPosition.get(i);
			if (parent == i) continue;
			addEdge(outputNode.get(parent), outputNode.get(i));
		}
		
		Map<EecbSrlAnnotation, Map<String, EecbSrlAnnotation>> semanticRoles = new HashMap<EecbSrlAnnotation, Map<String,EecbSrlAnnotation>>();
		for (Integer index : arguments.keySet()) {
			EecbSrlAnnotation annotation = annotations.get(index);
			Map<String, Integer> roles = arguments.get(index);
			Map<String, EecbSrlAnnotation> semRoles = new HashMap<String, EecbSrlAnnotation>();
			for (String role : roles.keySet()) {
				int headPosition = roles.get(role);
				EecbSrlAnnotation span = find(headPosition, annotations);
				span.setHeadStartIndex(headPosition);
				span.setHeadEndIndex(headPosition + 1);
				// find its yield according to headsPosition
				semRoles.put(role, span);
			}
			
			semanticRoles.put(annotation, semRoles);
		}
    	
    	return semanticRoles;
    }

    /**
	 * An Example on how to use this interface class. The input argument is the semantic parsing output result
	 * for example: test.output
	 * 1	With	with	with	IN	IN	_	_	13	13	ADV	ADV	_	_	_	_	AM-ADV
	 * 2	the	the	the	DT	DT	_	_	3	3	NMOD	NMOD	_	_	_	_	_
	 * 3	passing	passing	passing	NN	NN	_	_	1	1	PMOD	PMOD	Y	passing.01	_	_	_
	 * 4	of	of	of	IN	IN	_	_	3	3	NMOD	NMOD	_	_	A1	_	_
	 * 5	the	the	the	DT	DT	_	_	6	6	NMOD	NMOD	_	_	_	_	_
	 * 6	years	years	years	NNS	NNS	_	_	4	4	PMOD	PMOD	_	_	_	_	_
	 * 7	,	,	,	,	,	_	_	13	13	P	P	_	_	_	_	_
	 * 8	the	the	the	DT	DT	_	_	12	12	NMOD	NMOD	_	_	_	_	_
	 * 9	Sixties	sixty	sixty	NNS	NNS	_	_	10	10	NMOD	NMOD	_	_	_	A1	_
	 * 10	working-class	working-class	working-class	NN	NN	_	_	11	11	NMOD	NMOD	_	_	_	A1	_
	 * 11	wonder	wonder	wonder	NN	NN	_	_	12	12	NMOD	NMOD	Y	wonder.01	_	_	_
	 * 12	boy	boy	boy	NN	NN	_	_	13	13	SBJ	SBJ	_	_	_	_	A1
	 * 13	has	have	have	VBZ	VBZ	_	_	0	0	ROOT	ROOT	_	_	_	_	_
	 * 14	metamorphosed	metamorphose	metamorphose	VBN	VBN	_	_	13	13	VC	VC	Y	metamorphose.01	_	_	_
	 * 15	into	into	into	IN	IN	_	_	14	14	ADV	ADV	_	_	_	_	A2
	 * 16	a	a	a	DT	DT	_	_	19	19	NMOD	NMOD	_	_	_	_	_
	 * 17	very	very	very	RB	RB	_	_	18	18	AMOD	AMOD	_	_	_	_	_
	 * 18	cross	cross	cross	JJ	JJ	_	_	19	19	NMOD	NMOD	_	_	_	_	_
	 * 19	pensioner	pensioner	pensioner	NN	NN	_	_	15	15	PMOD	PMOD	_	_	_	_	_
	 * 20	.	.	.	.	.	_	_	13	13	P	P	_	_	_	_	_
	 * 
	 * @param args
	 */
	public static void main(String[] args) {		
		 String file = "../corpus/tokenoutput/16.output";
		 SRLExtraction semantic = new SRLExtraction();
		 semantic.setDocument(semantic.read(file));
		 Map<Integer, List<List<String>>> doc = semantic.getDocument();
		 
		 Map<Integer, Map<EecbSrlAnnotation, Map<String, EecbSrlAnnotation>>> extentsWithArgumentRoles = new HashMap<Integer, Map<EecbSrlAnnotation,Map<String,EecbSrlAnnotation>>>();
		 Map<Integer, List<String>> sentenceidToSentence = new HashMap<Integer, List<String>>();
		 for (Integer id : doc.keySet()) {
			 List<List<String>> sentence = doc.get(id);
			 Map<EecbSrlAnnotation, Map<String, EecbSrlAnnotation>> extentWithArgumentRoles = semantic.extractExtent(sentence);
			 if (extentWithArgumentRoles.size() == 0) continue;
			 List<String> tokens = new ArrayList<String>();
			 for (List<String> data : sentence) {
				tokens.add(data.get(1));
			 }
			 System.out.println(id);
			 System.out.println(tokens);
			 System.out.println(extentWithArgumentRoles);
			 System.out.println("\n");
			 extentsWithArgumentRoles.put(id , extentWithArgumentRoles);
			 sentenceidToSentence.put(id, tokens);
		 }
	}
	
	/**
	 * READ the Semantic Parsing output
	 * 
	 * @param path
	 * @return
	 */
	public Map<Integer, List<List<String>>> read(String path) {
        Map<Integer, List<List<String>>> datas = new HashMap<Integer, List<List<String>>>(); 
        try {
        		int sentenceOffset = 0;
                BufferedReader reader = new BufferedReader(new FileReader(path));
                List<List<String>> data = new ArrayList<List<String>>();
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                	if (line.equals("")) {
                		sentenceOffset += 1;
                		data = new ArrayList<List<String>>();
                	} else {
                		String[] records = line.split("\t");
                		List<String> datum = new ArrayList<String>();
                        records[0] = Integer.toString(Integer.parseInt(records[0]) - 1);
                        for (String record : records) {
                        	datum.add(record);
                        }
                        data.add(datum);
                	}
                	datas.put(sentenceOffset, data);
                }
                reader.close();
        } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
        }
        return datas;
	}
	
}
