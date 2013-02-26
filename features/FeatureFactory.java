package edu.oregonstate.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.search.State;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * generate feature set
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class FeatureFactory {
	
	// feature name
	private List<String> mFeatureNames;
	
	// Common indicator : nominal
	private final String COMMON = "-COMMON";
	
	// Proper indicator : proper
	private final String PROPER = "-PROPER";
	
	public FeatureFactory() {
		mFeatureNames = new ArrayList<String>();
	}
	
	public List<String> getFeatureNames() {
		return mFeatureNames;
	}
	
	private void Head() {
		String atomFeature = "Head";
		addFeature(atomFeature, false, true, true);
	}
	
	private void Lemma() {
		mFeatureNames.add("Lemma");
	}
	
	private void Synonym() {
		String atomFeature = "Synonym";
		addFeature(atomFeature, true, true, true);
	}
	
	private void SrlAgreeCount() {
		String atomFeature = "SrlAgreeCount";
		addFeature(atomFeature, true, true, true);
	}
	
	private void SrlA0() {
		String atomFeature = "SrlA0";
		mFeatureNames.add(atomFeature);
		mFeatureNames.add(atomFeature + PROPER);
		mFeatureNames.add(atomFeature + COMMON);
	}
	
	private void SrlA1() {
		String atomFeature = "SrlA1";
		addFeature(atomFeature, true, true, true);
	}
	
	private void SrlA2() {
		String atomFeature = "SrlA2";
		addFeature(atomFeature, true, true, true);
	}
	
	private void SrlAMLoc() {
		String atomFeature = "SrlAM-LOC";
		addFeature(atomFeature, true, true, true);
	}

	private void SrlPA0() {
		String atomFeature = "SrlPA0";
		addFeature(atomFeature, false, true, true);
	}
	
	private void SrlPA1() {
		String atomFeature = "SrlPA1";
		addFeature(atomFeature, false, true, true);
	}
	
	private void SrlPA2() {
		String atomFeature = "SrlPA2";
		addFeature(atomFeature, false, true, true);
	}
	
	private void SrlPAMLoc() {
		String atomFeature = "SrlPAM-LOC";
		addFeature(atomFeature, false, true, true);
	}
	
	private void NSrlAgreeCount() {
		String atomFeature = "NSrlAgreeCount";
		addFeature(atomFeature, true, true, true);
	}
	
	private void NSrlA0() {
		String atomFeature = "NSrlA0";
		mFeatureNames.add(atomFeature);
		mFeatureNames.add(atomFeature + PROPER);
		mFeatureNames.add(atomFeature + COMMON);
	}
	
	private void NSrlA1() {
		String atomFeature = "NSrlA1";
		addFeature(atomFeature, true, true, true);
	}
	
	private void NSrlA2() {
		String atomFeature = "NSrlA2";
		addFeature(atomFeature, true, true, true);
	}
	
	private void NSrlAMLoc() {
		String atomFeature = "NSrlAM-LOC";
		addFeature(atomFeature, true, true, true);
	}

	private void NSrlPA0() {
		String atomFeature = "NSrlPA0";
		addFeature(atomFeature, false, true, true);
	}
	
	private void NSrlPA1() {
		String atomFeature = "NSrlPA1";
		addFeature(atomFeature, false, true, true);
	}
	
	private void NSrlPA2() {
		String atomFeature = "NSrlPA2";
		addFeature(atomFeature, false, true, true);
	}
	
	private void NSrlPAMLoc() {
		String atomFeature = "NSrlPAM-LOC";
		addFeature(atomFeature, false, true, true);
	}
	
	private void MentionWord() {
		String atomFeature = "MentionWord";
		addFeature(atomFeature, false, true, true);
	}
	
	private void NEType() {
		String atomFeature = "NEType";
		addFeature(atomFeature, false, true, true);
	}
	
	private void Animacy() {
		String atomFeature = "Animacy";
		addFeature(atomFeature, false, true, true);
	}
	
	private void Gender() {
		String atomFeature = "Gender";
		addFeature(atomFeature, false, true, true);
	}
	
	private void Number() {
		String atomFeature = "Number";
		addFeature(atomFeature, false, true, true);
	}
	
	private void addFeature(String atomFeature, boolean verb, boolean proper, boolean common) {
		if (verb) {
			mFeatureNames.add(atomFeature);
		}
		
		if (proper) {
			mFeatureNames.add(atomFeature + PROPER);
		}
		
		if (common) {
			mFeatureNames.add(atomFeature + COMMON);
		}
	}
	
	/**
	 * add halt feature to feature template set if the stopping rate is halt
	 * 
	 * @return
	 */
	public static List<String> getFeatureTemplate() {
		String propertyKey = EecbConstants.FEATURE_ATOMIC_NAMES;
		String propertyValue = EecbConstants.FEATURE_NAMES;
		Properties mProps = ExperimentConstructor.experimentProps;
		
		String atomicFeatureNames = mProps.getProperty(propertyKey, propertyValue);
		String[] featureNames = atomicFeatureNames.split(",");
		
		FeatureFactory factory = new FeatureFactory();
		
		// add feature into the fullFeatureNames
		for (String name : featureNames) {
		
			// Head
			if (name.equals("Head")) {
				factory.Head();
			}
			
			// Lemma
			if (name.equals("Lemma")) {
				factory.Lemma();
			}
			
			// Synonym
			if (name.equals("Synonym")) {
				factory.Synonym();
			}
			
			// SrlAgreeCount
			if (name.equals("SrlAgreeCount")) {
				factory.SrlAgreeCount();
			}
			
			// SrlA0
			if (name.equals("SrlA0")) {
				factory.SrlA0();
			}
			
			// SrlA1
			if (name.equals("SrlA1")) {
				factory.SrlA1();
			}
			
			// SrlA2
			if (name.equals("SrlA2")) {
				factory.SrlA2();
			}
			
			if (name.equals("SrlAMLoc")) {
				factory.SrlAMLoc();
			}
			
			if (name.equals("SrlPA0")) {
				factory.SrlPA0();
			}
			
			if (name.equals("SrlPA1")) {
				factory.SrlPA1();
			}
			
			if (name.equals("SrlPA2")) {
				factory.SrlPA2();
			}
			
			if (name.equals("SrlPAMLoc")) {
				factory.SrlPAMLoc();
			}
			
			// NSrlAgreeCount
			if (name.equals("NSrlAgreeCount")) {
				factory.NSrlAgreeCount();
			}

			// NSrlA0
			if (name.equals("NSrlA0")) {
				factory.NSrlA0();
			}

			// NSrlA1
			if (name.equals("NSrlA1")) {
				factory.NSrlA1();
			}

			// NSrlA2
			if (name.equals("NSrlA2")) {
				factory.NSrlA2();
			}

			if (name.equals("NSrlAMLoc")) {
				factory.NSrlAMLoc();
			}

			if (name.equals("NSrlPA0")) {
				factory.NSrlPA0();
			}

			if (name.equals("NSrlPA1")) {
				factory.NSrlPA1();
			}

			if (name.equals("NSrlPA2")) {
				factory.NSrlPA2();
			}

			if (name.equals("NSrlPAMLoc")) {
				factory.NSrlPAMLoc();
			}
			
			if (name.equals("MentionWord")) {
				factory.MentionWord();
			}
			
			if (name.equals("NEType")) {
				factory.NEType();
			}
			
			if (name.equals("Animacy")) {
				factory.Animacy();
			}
			
			if (name.equals("Gender")) {
				factory.Gender();
			}
			
			if (name.equals("Number")) {
				factory.Number();
			}
			
		}
		
		return factory.getFeatureNames();
	}
	
}
