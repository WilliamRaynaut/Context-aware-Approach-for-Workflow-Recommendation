package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.openml.apiconnector.xml.RunList.Run;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.EvaluationList.Evaluation;

public class Baseline {

	@SuppressWarnings("unchecked")
	public void alwaysBest(String runFile, OpenmlConnector openML) throws Exception {

		// get goodRuns
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load(runFile);
		HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("wfDissimMatrix");

		HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
		filters.put("run", goodRuns);

		Run[] runTable = openML.runList(filters, null, null).getRuns();

		HashMap<Integer, Double> alwaysBestEvals = new HashMap<Integer, Double>();

		// get evaluations
//		Evaluation[] auc = openML.evaluationList(goodRuns, null, null, "area_under_roc_curve").getEvaluations();
//		Evaluation[] kappa = openML.evaluationList(goodRuns, null, null, "kappa").getEvaluations();
//		Evaluation[] info = openML.evaluationList(goodRuns, null, null, "kb_relative_information_score").getEvaluations();
		Evaluation[] acc = openML.evaluationList(goodRuns, null, null, "predictive_accuracy").getEvaluations();

		// find BEST run for each eval measure
		int bestAccRun = 0;
		int secondBestRun = 0;
		double best = -Double.MAX_VALUE;
		for (Evaluation e : acc) {
			if (e.getValue() > best && goodRuns.contains(e.getRun_id())) {
				secondBestRun = bestAccRun;
				bestAccRun = e.getRun_id();
			}
		}
//		int bestAucRun = 0;
//		best = -Double.MAX_VALUE;
//		for (Evaluation e : auc) {
//			if (e.getValue() > best && goodRuns.contains(e.getRun_id()))
//				bestAucRun = e.getRun_id();
//		}
//		int bestKappaRun = 0;
//		best = -Double.MAX_VALUE;
//		for (Evaluation e : kappa) {
//			if (e.getValue() > best && goodRuns.contains(e.getRun_id()))
//				bestKappaRun = e.getRun_id();
//		}
//		int bestInfoRun = 0;
//		best = -Double.MAX_VALUE;
//		for (Evaluation e : info) {
//			if (e.getValue() > best && goodRuns.contains(e.getRun_id()))
//				bestInfoRun = e.getRun_id();
//		}
		

		// foreach run
		int i = 1;
		for (int run : goodRuns) {
			Controller.LOGGER.log(Level.FINER, "For run " + run);

			// find BEST run for eval measure
//			int alwaysBestRun = 0;
//			for (TaskInputs.Input input : openML.taskInputs(run.getTask_id()).getInput()) {
//				if (input.getName().equals("evaluation_measures")) {
//					if (input.getValue().equals("area_under_roc_curve")) {
//						alwaysBestRun = bestAucRun;
//					} else if (input.getValue().equals("kappa")) {
//						alwaysBestRun = bestKappaRun;
//					} else if (input.getValue().equals("kb_relative_information_score")) {
//						alwaysBestRun = bestInfoRun;
//					} else if (input.getValue().equals("predictive_accuracy")) {
//						alwaysBestRun = bestAccRun;
//					} else {
//						throw new Exception("Discard, evaluation_measures is " + input.getValue());
//					}
//				}
//			}
			
			int alwaysBestRun = bestAccRun;

			// recommend wf : alwaysBestRun.getFlow_id()
			// evaluate recomended wf by dissim to run wf : run.getFlow_id() <-> closestRun.getFlow_id()
			double d;
			if (run == alwaysBestRun) {
				d = wfDissimMatrix.get(run).get(secondBestRun);
			} else {
				d = wfDissimMatrix.get(run).get(alwaysBestRun);
			}
			alwaysBestEvals.put(run, d);
			Controller.save(alwaysBestEvals, "alwaysBestEvals");

			Controller.LOGGER.log(Level.FINER, "alwaysBest gives run " + alwaysBestRun + ", scoring " + d);

			Controller.LOGGER.log(Level.FINE, "********************************************" + i + " / " + runTable.length + " done");
			i++;
		}

		Controller.save(alwaysBestEvals, "alwaysBestEvals");
	}

	@SuppressWarnings("unchecked")
	public void random(String runFile, OpenmlConnector openML) throws Exception {

		// get goodRuns
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load(runFile);
		HashMap<Integer, HashMap<Integer, Double>> wfDissimMatrix = (HashMap<Integer, HashMap<Integer, Double>>) Controller.load("wfDissimMatrix");

		HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
		filters.put("run", goodRuns);

		Run[] runTable = openML.runList(filters, null, null).getRuns();

		HashMap<Integer, Double> randomEvals = new HashMap<Integer, Double>();

		Random randomGenerator = new Random();

		// foreach run
		int i = 1;
		for (Run run : runTable) {
			Controller.LOGGER.log(Level.FINER, "For run " + run.getRun_id());

			double d = 0.0;
			for (int j = 0; j < 5; j++) {
				Run randomRun = run;
				while (run.getFlow_id() == randomRun.getFlow_id()) {
					randomRun = runTable[(randomGenerator.nextInt(runTable.length))];
				}
				double dissim = wfDissimMatrix.get(run.getRun_id()).get(randomRun.getRun_id());
				Controller.LOGGER.log(Level.FINER, "rnd gives run " + randomRun.getRun_id() + ", scoring " + dissim);
				d += dissim;
			}
			d /= 5;

			randomEvals.put(run.getRun_id(), d);
			Controller.save(randomEvals, "randomEvals");

			Controller.LOGGER.log(Level.FINER, "random scores " + d);

			Controller.LOGGER.log(Level.FINE, "********************************************" + i + " / " + runTable.length + " done");
			i++;
		}

		Controller.save(randomEvals, "randomEvals");
	}

}
