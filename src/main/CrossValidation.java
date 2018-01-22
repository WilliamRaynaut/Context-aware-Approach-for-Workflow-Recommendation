package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.openml.apiconnector.io.OpenmlConnector;
import structures.SortedMap;

public class CrossValidation {

	@SuppressWarnings("unchecked")
	public void computeEval(String runFile, OpenmlConnector openML) throws Exception {

		Baseline baseline = new Baseline();
		baseline.alwaysBest(runFile, openML);
		baseline.random(runFile, openML);
		paretoReco(runFile, openML);
		meanReco(runFile, openML);

		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load(runFile);
		HashMap<Integer, Double> meanRecoEvals = (HashMap<Integer, Double>) Controller.load("meanRecoEvals");
		HashMap<Integer, Double> paretoRecoEvals = (HashMap<Integer, Double>) Controller.load("paretoRecoEvals");
		HashMap<Integer, Double> alwaysBestEvals = (HashMap<Integer, Double>) Controller.load("alwaysBestEvals");
		HashMap<Integer, Double> randomEvals = (HashMap<Integer, Double>) Controller.load("randomEvals");
		//System.out.println("seuil" + ", " + "meanReco" + ", " + "paretoReco" + ", " + "alwaysBest" + ", " + "random");

		double threshold = 0.05;
		while (threshold < 10) {

			double meanRecoTP = 0.0;
			double meanRecoFP = 0.0;
			double paretoRecoTP = 0.0;
			double paretoRecoFP = 0.0;
			double alwaysBestTP = 0.0;
			double alwaysBestFP = 0.0;
			double randomTP = 0.0;
			double randomFP = 0.0;
			for (int run : goodRuns) {
				if (meanRecoEvals.get(run) <= threshold) {
					meanRecoTP++;
				} else {
					meanRecoFP++;
				}
				if (paretoRecoEvals.get(run) <= threshold) {
					paretoRecoTP++;
				} else {
					paretoRecoFP++;
				}
				if (alwaysBestEvals.get(run) <= threshold) {
					alwaysBestTP++;
				} else {
					alwaysBestFP++;
				}
				if (randomEvals.get(run) <= threshold) {
					randomTP++;
				} else {
					randomFP++;
				}
			}
			double meanReco = meanRecoTP / (meanRecoTP + meanRecoFP);
			double paretoReco = paretoRecoTP / (paretoRecoTP + paretoRecoFP);
			double alwaysBest = alwaysBestTP / (alwaysBestTP + alwaysBestFP);
			double random = randomTP / (randomTP + randomFP);

			System.out.println(threshold + "	" + meanReco + "	" + paretoReco + "	" + alwaysBest + "	" + random);
			threshold += 0.05;
		}
	}

	@SuppressWarnings("unchecked")
	public void paretoReco(String runFile, OpenmlConnector openML) throws Exception {

		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load(runFile);
		HashMap<Integer, HashMap<Integer, Double>> dataDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("dataDissimMatrix");
		HashMap<Integer, HashMap<Integer, Double>> evalDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("evalDissimMatrix");
		HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("wfDissimMatrix");

		HashMap<Integer, Double> recoEvals = new HashMap<Integer, Double>();

		// foreach run
		int i = 1;
		for (int run : goodRuns) {
			Controller.LOGGER.log(Level.FINER, "Mapped run " + run);

			// find pareto runs
			ArrayList<Integer> paretoRuns = new ArrayList<Integer>();
			HashMap<Integer, Double> dataDissimVector = dataDissimMatrix.get(run);
			HashMap<Integer, Double> evalDissimVector = evalDissimMatrix.get(run);
			// from the closest run by data
			SortedMap<Integer> sortedDataDissimVector = new SortedMap<Integer>(dataDissimVector.size(), true);
			for (int rid : dataDissimVector.keySet()) {
				sortedDataDissimVector.add(dataDissimVector.get(rid), rid);
			}
			paretoRuns.add(sortedDataDissimVector.getValue(0));
			// then by increasing dissim to data, add runs that get closer in eval
			int lastPareto = sortedDataDissimVector.getValue(0);
			for (int j = 1; j < sortedDataDissimVector.size(); j++) {
				if (evalDissimVector.get(sortedDataDissimVector.getValue(j)) < evalDissimVector.get(lastPareto)) {
					paretoRuns.add(sortedDataDissimVector.getValue(j));
					lastPareto = sortedDataDissimVector.getValue(j);
				}
			}

			// recomend the best of them
			HashMap<Integer, Double> wfDissimVector = wfDissimMatrix.get(run);
			int closestRun = 0;
			double closestWfDissim = Double.MAX_VALUE;
			for (int r2 : paretoRuns) {
				if (wfDissimVector.get(r2) < closestWfDissim) {
					closestRun = r2;
					closestWfDissim = wfDissimVector.get(r2);
				}
			}

			// recommend wf for dataset + pref profile : closestRun.getFlow_id()
			// evaluate recomended wf by dissim to run wf : run.getFlow_id() <-> closestRun.getFlow_id()
			// WorkflowDissimilarity workflowDissimilarity = new WorkflowDissimilarity();
			// double d = workflowDissimilarity.computeDissimilarity(run.getFlow_id(), closestRun.getFlow_id(), openML);

			recoEvals.put(run, closestWfDissim);

			Controller.LOGGER.log(Level.FINER, "to run " + closestRun + ", scoring " + closestWfDissim);
			Controller.LOGGER.log(Level.FINE, "********************************************" + i + " / " + goodRuns.size() + " done");
			i++;
		}

		Controller.save(recoEvals, "paretoRecoEvals");
	}

	@SuppressWarnings("unchecked")
	public void meanReco(String runFile, OpenmlConnector openML) throws Exception {

		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load(runFile);
		HashMap<Integer, HashMap<Integer, Double>> dataDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("dataDissimMatrix");
		HashMap<Integer, HashMap<Integer, Double>> evalDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("evalDissimMatrix");
		HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("wfDissimMatrix");

		HashMap<Integer, Double> recoEvals = new HashMap<Integer, Double>();

		// foreach run
		int i = 1;
		for (int run : goodRuns) {
			Controller.LOGGER.log(Level.FINER, "Mapped run " + run);

			HashMap<Integer, Double> dataDissimVector = dataDissimMatrix.get(run);
			HashMap<Integer, Double> evalDissimVector = evalDissimMatrix.get(run);
			HashMap<Integer, Double> wfDissimVector = wfDissimMatrix.get(run);

			int closestRun = 0;
			double closestDissim = Double.MAX_VALUE;
			for (int r2 : goodRuns) {
				if (r2 != run) {
					double d = (dataDissimVector.get(r2) + evalDissimVector.get(r2)) / 2;
					if (d < closestDissim) {
						closestRun = r2;
						closestDissim = d;
					}
				}
			}

			recoEvals.put(run, wfDissimVector.get(closestRun));

			Controller.LOGGER.log(Level.FINER, "to run " + closestRun + ", scoring " + wfDissimVector.get(closestRun));
			Controller.LOGGER.log(Level.FINE, "********************************************" + i + " / " + goodRuns.size() + " done");
			i++;
		}

		Controller.save(recoEvals, "meanReco");
	}
}
