package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.openml.apiconnector.xml.TaskInputs;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.RunList.Run;
import org.openml.apiconnector.xml.Tasks.Task;
import org.openml.apiconnector.xml.Tasks.Task.Input;

import structures.SortedMap;
import structures.UserPreferences;
import wfDissim.WorkflowDissimilarity;

public class DissimMats {

	@SuppressWarnings("unchecked")
	public void allSplits(OpenmlConnector openML) throws Exception {
		HashMap<Integer, HashMap<Integer, Double>> dataDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("dataDissimMatrix");
		HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("wfDissimMatrix");
		HashMap<Integer, HashMap<Integer, Double>> evalDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("evalDissimMatrix");

		Controller.LOGGER.log(Level.FINE, "Split on data dissimilarity");
		splitSet(openML, dataDissimMatrix, "data");

		Controller.LOGGER.log(Level.FINE, "Split on workflow dissimilarity");
		splitSet(openML, wfDissimMatrix, "wf");

		Controller.LOGGER.log(Level.FINE, "Split on evaluation dissimilarity");
		splitSet(openML, evalDissimMatrix, "eval");
	}

	public void splitSet(OpenmlConnector openML, HashMap<Integer, HashMap<Integer, Double>> dissimMatrix, String name) throws Exception {
		// find "median" run
		// int medianRun = 0;
		// double sumDissim = Double.MAX_VALUE;
		// for (int run : dissimMatrix.keySet()) {
		// double sum = 0;
		// for (int r2 : dissimMatrix.get(run).keySet()) {
		// sum += dissimMatrix.get(run).get(r2);
		// }
		// if (sum < sumDissim) {
		// sumDissim = sum;
		// medianRun = run;
		// }
		// }

		double best = 0;
		for (int medianRun : dissimMatrix.keySet()) {
			// rank runs by dist to median
			SortedMap<Integer> ranking = new SortedMap<Integer>(dissimMatrix.keySet().size(), false);
			ranking.add(0, medianRun);
			for (int run : dissimMatrix.keySet()) {
				if (run != medianRun)
					ranking.add(dissimMatrix.get(run).get(medianRun), run);
			}

			// split
			ArrayList<Integer> closest = new ArrayList<Integer>();
			ArrayList<Integer> farthest = new ArrayList<Integer>();
			for (int i = 0; i < ranking.size(); i++) {
				if (i < (ranking.size() / 2.0)) {
					farthest.add(ranking.getValue(i));
				} else {
					closest.add(ranking.getValue(i));
				}
			}

			// compute mean internal dissims
			double fullSum = 0.0;
			int fullNb = 0;
			double closestSum = 0.0;
			int closestNb = 0;
			double farthestSum = 0.0;
			int farthestNb = 0;
			for (int r1 : dissimMatrix.keySet()) {
				for (int r2 : dissimMatrix.get(r1).keySet()) {
					fullSum += dissimMatrix.get(r1).get(r2);
					fullNb++;
					if (closest.contains(r1) && closest.contains(r2)) {
						closestSum += dissimMatrix.get(r1).get(r2);
						closestNb++;
					}
					if (farthest.contains(r1) && farthest.contains(r2)) {
						farthestSum += dissimMatrix.get(r1).get(r2);
						farthestNb++;
					}
				}
			}
			double fullMean = fullSum / fullNb;
			double closestMean = closestSum / closestNb;
			double farthestMean = farthestSum / farthestNb;

			if (farthestMean - closestMean > best) {
				best=farthestMean - closestMean;
				
				Controller.LOGGER.log(Level.FINE, "*******************************************");
				Controller.LOGGER.log(Level.FINE, "closest set : " + closest.toString());
				Controller.LOGGER.log(Level.FINE, "farthest set : " + farthest.toString());
				Controller.save(closest, name + "closestRuns");
				Controller.save(farthest, name + "farthestRuns");
				Controller.LOGGER.log(Level.FINE, "mean internal dissim on full set : " + fullMean);
				Controller.LOGGER.log(Level.FINE, "mean internal dissim on closest set : " + closestMean);
				Controller.LOGGER.log(Level.FINE, "mean internal dissim on farthest set : " + farthestMean);
			}
			
		}
	}

	@SuppressWarnings("unchecked")
	public void checkDissimMatrix(OpenmlConnector openML) throws Exception {

		HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("wfDissimMatrix");
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load("goodRuns");
		WorkflowDissimilarity workflowDissimilarity = new WorkflowDissimilarity();
		for (int i : goodRuns) {
			// System.out.println(wfDissimMatrix.get(i).get(i));
			for (int j : goodRuns) {
				if (i != j && !Double.isFinite(wfDissimMatrix.get(i).get(j))) {
					Controller.LOGGER.log(Level.FINE, i + " +++++++++ " + j);
					System.out.println(i + " +++++++++ " + j);
					int flow1 = openML.runGet(i).getFlow_id();
					int flow2 = openML.runGet(j).getFlow_id();
					double d = 0;
					if (flow1 != flow2)
						d = workflowDissimilarity.computeDissimilarity(flow2, flow1, openML);
					Controller.LOGGER.log(Level.FINE, d + "");
					System.out.println(d);
					wfDissimMatrix.get(i).put(j, d);
					Controller.save(wfDissimMatrix, "wfDissimMatrix");
				}

			}
		}

	}

	@SuppressWarnings("unchecked")
	public void buildWfissimMatrix(OpenmlConnector openML) throws Exception {

		HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("wfDissimMatrix");
		// HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = new HashMap<Integer, HashMap<Integer, Double>>();
		WorkflowDissimilarity workflowDissimilarity = new WorkflowDissimilarity();

		// get goodRuns
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load("goodRuns");

		HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
		filters.put("run", goodRuns);

		Run[] runTable = openML.runList(filters, null, null).getRuns();
		int pbSize = runTable.length * runTable.length;
		int current = 0;

		// foreach run
		for (Run run : runTable) {
			Controller.LOGGER.log(Level.FINEST, "From run " + run.getRun_id() + ", task " + run.getTask_id());
			HashMap<Integer, Double> dissimVector = new HashMap<Integer, Double>();
			if (wfDissimMatrix.containsKey(run.getRun_id())) {
				dissimVector = wfDissimMatrix.get(run.getRun_id());
			}

			// foreach other run id
			for (Run run2 : runTable) {
				if (run2.getRun_id() > run.getRun_id() && !dissimVector.containsKey(run2.getRun_id())) {
					// compute dissim
					Controller.LOGGER.log(Level.FINEST, "to run " + run2.getRun_id() + ", task " + run2.getTask_id());

					double d = workflowDissimilarity.computeDissimilarity(run.getFlow_id(), run2.getFlow_id(), openML);
					dissimVector.put(run2.getRun_id(), d);

					Controller.LOGGER.log(Level.FINEST, "gives : " + d);
				}
				Controller.LOGGER.log(Level.FINE, current++ + " / " + pbSize + " done");
			}

			wfDissimMatrix.put(run.getRun_id(), dissimVector);
			Controller.save(wfDissimMatrix, "wfDissimMatrix");
		}

		for (int run : wfDissimMatrix.keySet()) {
			System.out.println(run + " : ");
			for (int run2 : wfDissimMatrix.get(run).keySet()) {
				System.out.print(run2);
				wfDissimMatrix.get(run2).put(run, wfDissimMatrix.get(run).get(run2));
			}
		}

		Controller.save(wfDissimMatrix, "wfDissimMatrix");
	}

	public void buildDataDissimMatrix(OpenmlConnector openML) throws Exception {

		HashMap<Integer, HashMap<Integer, Double>> dataDissimMatrix = new HashMap<Integer, HashMap<Integer, Double>>();
		HashMap<Integer, HashMap<Integer, Long>> dataDissimTimes = new HashMap<Integer, HashMap<Integer, Long>>();
		Dissimilarity dissimilarity = Dissimilarity.defaultDissimilarity();

		// get goodRuns
		@SuppressWarnings("unchecked")
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load("goodRuns");

		HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
		filters.put("run", goodRuns);

		Run[] runTable = openML.runList(filters, null, null).getRuns();
		int pbSize = runTable.length * runTable.length - runTable.length;
		int current = 0;

		// foreach run
		for (Run run : runTable) {
			Controller.LOGGER.log(Level.FINER, "From run " + run.getRun_id() + ", task " + run.getTask_id());
			HashMap<Integer, Double> dissimVector = new HashMap<Integer, Double>();
			HashMap<Integer, Long> timesVector = new HashMap<Integer, Long>();

			// foreach other run id
			for (Run run2 : runTable) {
				if (run2.getRun_id() != run.getRun_id()) {
					// compute dissim
					Controller.LOGGER.log(Level.FINEST, "to run " + run2.getRun_id() + ", task " + run2.getTask_id());
					long start_time = System.nanoTime();
					double d = dissimilarity.computeDataDissimilarity(run.getTask_id(), run2.getTask_id());
					long end_time = System.nanoTime();
					timesVector.put(run2.getRun_id(), end_time - start_time);
					dissimVector.put(run2.getRun_id(), d);

					Controller.LOGGER.log(Level.FINER, "to run " + run2.getRun_id() + " : " + d);
					System.out.println(current++ + " / " + pbSize + " done");
				}
			}

			dataDissimMatrix.put(run.getRun_id(), dissimVector);
			dataDissimTimes.put(run.getRun_id(), timesVector);
		}

		Controller.save(dataDissimMatrix, "dataDissimMatrix");
		Controller.save(dataDissimTimes, "dissimTimes");
	}

	public void buildEvalDissimMatrix(OpenmlConnector openML) throws Exception {

		HashMap<Integer, HashMap<Integer, Double>> evalDissimMatrix = new HashMap<Integer, HashMap<Integer, Double>>();
		Dissimilarity dissimilarity = Dissimilarity.defaultDissimilarity();

		// get goodRuns
		@SuppressWarnings("unchecked")
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load("goodRuns");

		HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
		filters.put("run", goodRuns);

		Task[] taskTable = openML.taskList("active", 1).getTask();
		Run[] runTable = openML.runList(filters, null, null).getRuns();
		int pbSize = runTable.length * runTable.length - runTable.length;
		int current = 0;

		// foreach run
		for (Run run : runTable) {
			Controller.LOGGER.log(Level.FINER, "From run " + run.getRun_id() + ", task " + run.getTask_id());
			HashMap<Integer, Double> dissimVector = new HashMap<Integer, Double>();

			// get task
			Task task = null;
			for (Task t : taskTable) {
				if (t.getTask_id() == run.getTask_id()) {
					task = t;
				}
			}

			// build pref profile
			UserPreferences prefs = new UserPreferences("area_under_roc_curve", true, 1.0, 0, 1);
			try {
				for (Input i : task.getInputs()) {
					if (i.getName().equals("evaluation_measures")) {
						if (i.getValue().equals("area_under_roc_curve")) {
							prefs = new UserPreferences("area_under_roc_curve", true, 1.0, 0, 1);
						} else if (i.getValue().equals("kappa")) {
							prefs = new UserPreferences("kappa", true, 1.0, 0, 1);
						} else if (i.getValue().equals("kb_relative_information_score")) {
							prefs = new UserPreferences("kb_relative_information_score", true, Double.valueOf("Infinity"), 0, 1);
						} else if (i.getValue().equals("predictive_accuracy")) {
							prefs = new UserPreferences("predictive_accuracy", true, 1.0, 0, 1);
						} else {
							throw new Exception("Discard, evaluation_measures is " + i.getValue());
						}
					}
				}
			} catch (Exception e) {
				for (TaskInputs.Input i : openML.taskInputs(run.getTask_id()).getInput()) {
					if (i.getName().equals("evaluation_measures")) {
						if (i.getValue().equals("area_under_roc_curve")) {
							prefs = new UserPreferences("area_under_roc_curve", true, 1.0, 0, 1);
						} else if (i.getValue().equals("kappa")) {
							prefs = new UserPreferences("kappa", true, 1.0, 0, 1);
						} else if (i.getValue().equals("kb_relative_information_score")) {
							prefs = new UserPreferences("kb_relative_information_score", true, Double.valueOf("Infinity"), 0, 1);
						} else if (i.getValue().equals("predictive_accuracy")) {
							prefs = new UserPreferences("predictive_accuracy", true, 1.0, 0, 1);
						} else {
							throw new Exception("Discard, evaluation_measures is " + i.getValue());
						}
					}
				}
			}

			// foreach other run id
			for (Run run2 : runTable) {
				if (run2.getRun_id() != run.getRun_id()) {
					// compute dissim
					Controller.LOGGER.log(Level.FINEST, "to run " + run2.getRun_id() + ", task " + run2.getTask_id());

					double evalDissim = dissimilarity.computeEvalDissimilarity(run2.getRun_id(), prefs);
					// double evalDissim = Math.abs(getEval(auc, run.getRun_id()) - getEval(auc, run2.getRun_id()));
					dissimVector.put(run2.getRun_id(), evalDissim);

					Controller.LOGGER.log(Level.FINER, "gives : " + evalDissim);
					System.out.println(current++ + " / " + pbSize + " done");
				}
			}

			evalDissimMatrix.put(run.getRun_id(), dissimVector);
		}

		Controller.save(evalDissimMatrix, "evalDissimMatrix");
	}

}
