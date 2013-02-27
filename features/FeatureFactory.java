package edu.oregonstate.features;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.util.EecbConstants;

/**
 * generate feature set
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class FeatureFactory {
	
	// feature name
	public List<String> mFeatureNames;
	
	// Common indicator : nominal
	private final String COMMON = "-NOMINAL";
	
	// Proper indicator : proper
	private final String PROPER = "-PROPER";
	
	public FeatureFactory() {
		mFeatureNames = new ArrayList<String>();
	}
	
	public List<String> getFeatureNames() {
		return mFeatureNames;
	}
	
	public void Head() {
		String atomFeature = "Head";
		addFeature(atomFeature, false, true, true);
	}
	
	public void Lemma() {
		mFeatureNames.add("Lemma");
	}
	
	public void Synonym() {
		String atomFeature = "Synonym";
		addFeature(atomFeature, true, true, true);
	}
	
	public void SrlAgreeCount() {
		String atomFeature = "SrlAgreeCount";
		addFeature(atomFeature, true, true, true);
	}
	
	public void SrlA0() {
		String atomFeature = "SrlA0";
		addFeature(atomFeature, true, true, true);
	}
	
	public void SrlA1() {
		String atomFeature = "SrlA1";
		addFeature(atomFeature, true, true, true);
	}
	
	public void SrlA2() {
		String atomFeature = "SrlA2";
		addFeature(atomFeature, true, true, true);
	}
	
	public void SrlAMLoc() {
		String atomFeature = "SrlAM-LOC";
		addFeature(atomFeature, true, true, true);
	}

	public void SrlPA0() {
		String atomFeature = "SrlPA0";
		addFeature(atomFeature, false, true, true);
	}
	
	public void SrlPA1() {
		String atomFeature = "SrlPA1";
		addFeature(atomFeature, false, true, true);
	}
	
	public void SrlPA2() {
		String atomFeature = "SrlPA2";
		addFeature(atomFeature, false, true, true);
	}
	
	public void SrlPAMLoc() {
		String atomFeature = "SrlPAM-LOC";
		addFeature(atomFeature, false, true, true);
	}
	
	public void NSrlAgreeCount() {
		String atomFeature = "NSrlAgreeCount";
		addFeature(atomFeature, true, true, true);
	}
	
	public void NSrlA0() {
		String atomFeature = "NSrlA0";
		addFeature(atomFeature, true, true, true);
	}
	
	public void NSrlA1() {
		String atomFeature = "NSrlA1";
		addFeature(atomFeature, true, true, true);
	}
	
	public void NSrlA2() {
		String atomFeature = "NSrlA2";
		addFeature(atomFeature, true, true, true);
	}
	
	public void NSrlAMLoc() {
		String atomFeature = "NSrlAM-LOC";
		addFeature(atomFeature, true, true, true);
	}

	public void NSrlPA0() {
		String atomFeature = "NSrlPA0";
		addFeature(atomFeature, false, true, true);
	}
	
	public void NSrlPA1() {
		String atomFeature = "NSrlPA1";
		addFeature(atomFeature, false, true, true);
	}
	
	public void NSrlPA2() {
		String atomFeature = "NSrlPA2";
		addFeature(atomFeature, false, true, true);
	}
	
	public void NSrlPAMLoc() {
		String atomFeature = "NSrlPAM-LOC";
		addFeature(atomFeature, false, true, true);
	}
	
	public void MentionWord() {
		String atomFeature = "MentionWord";
		addFeature(atomFeature, false, true, true);
	}
	
	public void NEType() {
		String atomFeature = "NEType";
		addFeature(atomFeature, false, true, true);
	}
	
	public void Animacy() {
		String atomFeature = "Animacy";
		addFeature(atomFeature, false, true, true);
	}
	
	public void Gender() {
		String atomFeature = "Gender";
		addFeature(atomFeature, false, true, true);
	}
	
	public void Number() {
		String atomFeature = "Number";
		addFeature(atomFeature, false, true, true);
	}
	
	public void addFeature(String atomFeature, boolean verb, boolean proper, boolean common) {
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
		
		boolean previous = true;
		List<String> features = null;
		
		if (previous) {
			features = Arrays.asList(EecbConstants.featuresName);
		} else {
			FeatureFactory factory = new FeatureFactory();
			
			// add feature into the fullFeatureNames
			for (String name : featureNames) {
				try {
					Method method = factory.getClass().getMethod(name, new Class[0]);
					method.invoke(factory, new Object[0]);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			
			features = factory.getFeatureNames();
		}
		
		return features;
	}
	
}
