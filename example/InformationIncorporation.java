package edu.oregonstate.example;

public class InformationIncorporation {

	private static void addMentionToCentroid(HashMap<String, ClassicCounter<String>> centroid, JMention m, Document doc, boolean gold) {
		// synonym and sentence words are compared in pairwise
		if(!centroid.containsKey("LEMMA")) centroid.put("LEMMA", new ClassicCounter<String>());
		centroid.get("LEMMA").incrementCount(m.headWord.get(LemmaAnnotation.class));
		if(m.isVerb) {
			for(String role : m.srlArgs.keySet()) {
				if(m.srlArgs.get(role)==null) continue;
				if(!centroid.containsKey("SRLROLES-"+role)) centroid.put("SRLROLES-"+role, new ClassicCounter<String>());
				if(gold) {
					if(doc.allGoldMentions.containsKey(m.srlArgs.get(role).mentionID)) {
						int goldID = doc.allGoldMentions.get(m.srlArgs.get(role).mentionID).goldCorefClusterID;
						centroid.get("SRLROLES-"+role).incrementCount(Integer.toString(goldID));
					}   
				} else {
					centroid.get("SRLROLES-"+role).incrementCount(Integer.toString(m.srlArgs.get(role).corefClusterID));
				}   
			}   
		} else {
			if(!centroid.containsKey("GENDER") && m.gender != Gender.UNKNOWN) centroid.put("GENDER", new ClassicCounter<String>());
			if(!centroid.containsKey("NUMBER") && m.number != Number.UNKNOWN) centroid.put("NUMBER", new ClassicCounter<String>());
			if(!centroid.containsKey("ANIMACY")&& m.animacy!= Animacy.UNKNOWN) centroid.put("ANIMACY", new ClassicCounter<String>());
			if(!centroid.containsKey("NETYPE") && !m.nerString.equals("O")) centroid.put("NETYPE", new ClassicCounter<String>());
			if(!centroid.containsKey("MENTION_WORDS")) centroid.put("MENTION_WORDS", new ClassicCounter<String>());
			if(!centroid.containsKey("HEAD") && !m.isPronominal()) centroid.put("HEAD", new ClassicCounter<String>());

			for(String role : m.srlArgs.keySet()) {
				if(m.srlArgs.get(role)==null) continue;
				if(!centroid.containsKey("SRLROLES-"+role)) centroid.put("SRLROLES-"+role, new ClassicCounter<String>());
				if(gold) {
					if(doc.allGoldMentions.containsKey(m.srlArgs.get(role).mentionID)) {
						int goldID = doc.allGoldMentions.get(m.srlArgs.get(role).mentionID).goldCorefClusterID;
						centroid.get("SRLROLES-"+role).incrementCount(Integer.toString(goldID));
					}   
				} else {
					centroid.get("SRLROLES-"+role).incrementCount(Integer.toString(m.srlArgs.get(role).corefClusterID));
				}   
			}   
			for(Mention mention : m.srlPredicates.keySet()) {
				String role = m.srlPredicates.get(mention);
				if(!centroid.containsKey("SRLPREDS-"+role)) centroid.put("SRLPREDS-"+role, new ClassicCounter<String>());
				if(gold) {
					if(doc.allGoldMentions.containsKey(mention.mentionID)) {
						int goldID = doc.allGoldMentions.get(mention.mentionID).goldCorefClusterID;
						centroid.get("SRLPREDS-"+role).incrementCount(Integer.toString(goldID));
					}   
				} else {
					centroid.get("SRLPREDS-"+role).incrementCount(Integer.toString(mention.corefClusterID));
				}   
			}   

			if(!m.isPronominal()) {
				centroid.get("HEAD").incrementCount(m.headWord.get(TextAnnotation.class));
			}   
			if(m.gender != Gender.UNKNOWN) centroid.get("GENDER").incrementCount(m.gender.toString());
			if(m.number != Number.UNKNOWN) centroid.get("NUMBER").incrementCount(m.number.toString());
			if(m.animacy!= Animacy.UNKNOWN) centroid.get("ANIMACY").incrementCount(m.animacy.toString());
			if(!m.nerString.equals("O")) centroid.get("NETYPE").incrementCount(m.nerString);

			Counters.addInPlace(centroid.get("MENTION_WORDS"), m.simVector.vector);   // 2nd order
		}   
	}

}
