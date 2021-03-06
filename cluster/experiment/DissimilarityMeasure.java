package edu.oregonstate.cluster.experiment;

import java.util.List;
import Jama.Matrix;
import edu.oregonstate.data.EecbClusterDocument;

/**
 * Computes the dissimilarity between two observations in an experiment.
 * 
 * @author Matthias.Hauswirth@usi.ch
 */
public interface DissimilarityMeasure {

    public double computeDissimilarity(List<EecbClusterDocument> vectors, int observation1, int observation2);

    public double cosineSimilarity(Matrix obs1, Matrix obs2);
}