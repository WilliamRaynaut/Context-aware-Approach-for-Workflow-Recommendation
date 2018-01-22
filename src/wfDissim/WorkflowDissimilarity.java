package wfDissim;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.EvaluationList.Evaluation;
import org.openml.apiconnector.xml.RunList.Run;
import org.openml.apiconnector.xml.Tasks.Task;
import org.openml.apiconnector.xml.Tasks.Task.Quality;

import main.Controller;
import main.Dissimilarity;

public class WorkflowDissimilarity {

	private final int maxNbFeatures = 500;
	private final int maxDatasetByteLength = 5000000;
	private final int nbRunsForSpearman = 50;

	public DescriptiveStatistics computeDissimilarity(ArrayList<Integer> flowIds, OpenmlConnector openML) throws Exception {
		DescriptiveStatistics dissims = new DescriptiveStatistics();
		for (int i = 0; i < flowIds.size(); i++) {
			Controller.LOGGER.log(Level.FINER, i + " / " + flowIds.size());
			for (int j = i + 1; j < flowIds.size(); j++) {
				double d = computeDissimilarity(flowIds.get(i), flowIds.get(j), openML);
				dissims.addValue(d);
			}
		}
		return dissims;
	}

	public double computeDissimilarity(int flowId1, int flowId2, OpenmlConnector openML) throws Exception {

		Task[] taskTable = openML.taskList(1).getTask();
		Task[] downTaskTable = openML.taskList("deactivated", 1).getTask();
		Dissimilarity dissimilarity = Dissimilarity.defaultDissimilarity();

		// get oml runs with flows
		HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
		ArrayList<Integer> tmp = new ArrayList<Integer>();
		tmp.add(flowId1);
		filters.put("flow", tmp);
		Run[] runTable1 = openML.runList(filters, 2 * nbRunsForSpearman, null).getRuns();
		filters = new HashMap<String, List<Integer>>();
		tmp = new ArrayList<Integer>();
		tmp.add(flowId2);
		filters.put("flow", tmp);
		Run[] runTable2 = openML.runList(filters, 2 * nbRunsForSpearman, null).getRuns();

		ArrayList<Integer> runIds1 = new ArrayList<Integer>();
		for (Run r : runTable1) {
			if (!runIds1.contains(r.getRun_id()))
				runIds1.add(r.getRun_id());
		}

		ArrayList<Integer> runIds2 = new ArrayList<Integer>();
		for (Run r : runTable2) {
			if (!runIds2.contains(r.getRun_id()))
				runIds2.add(r.getRun_id());
		}

		Evaluation[] auc1 = openML.evaluationList(runIds1, null, null, "area_under_roc_curve").getEvaluations();
		Evaluation[] auc2 = openML.evaluationList(runIds2, null, null, "area_under_roc_curve").getEvaluations();

		ArrayList<Run> runList1 = new ArrayList<Run>();
		ArrayList<Run> runList2 = new ArrayList<Run>();
		ArrayList<Pair> dualDissimilarities = new ArrayList<Pair>();

		// get clean runs
		for (Run r1 : runTable1) {
			try {
				if (runList1.size() >= nbRunsForSpearman)
					break;

				// check r1
				// if missing eval discard
				if (!hasEval(auc1, r1.getRun_id())) {
					throw new Exception("Missing eval");
				}
				
				//if too many same task discard
				int nb=0;
				for (Run rl : runList1) {
					if(rl.getTask_id()==r1.getTask_id()) nb++;
				}
				if (nb>5) {
					throw new Exception("too many same task");
				}

				// check dataset
				int did1 = 0;
				boolean found = false;
				for (Task t : taskTable) {
					if (t.getTask_id() == r1.getTask_id()) {
						// if >500 features discard
						for (Quality q : t.getQualities()) {
							if (q.getName().equals("NumberOfFeatures") && Double.parseDouble(q.getValue()) > maxNbFeatures) {
								throw new Exception("Discard, too many features, " + Double.parseDouble(q.getValue()) + ">" + maxNbFeatures);
							}
						}
						did1 = t.getDid();
						found = true;
					}
				}
				if (!found) {
					for (Task t : downTaskTable) {
						if (t.getTask_id() == r1.getTask_id()) {
							// if >500 features discard
							for (Quality q : t.getQualities()) {
								if (q.getName().equals("NumberOfFeatures") && Double.parseDouble(q.getValue()) > maxNbFeatures) {
									throw new Exception("Discard, too many features, " + Double.parseDouble(q.getValue()) + ">" + maxNbFeatures);
								}
							}
							did1 = t.getDid();
							found = true;
						}
					}
					if (!found)
						throw new Exception("fucked task " + r1.getTask_id());
				}

				DataSetDescription desc1 = openML.dataGet(did1);
				// if unavailable or not arff discard
				if (!desc1.getVisibility().equals("public") || !desc1.getFormat().equals("ARFF")) {
					throw new Exception(desc1.getVisibility() + ", " + desc1.getFormat());
				}
				// actual download
				File dataset1 = desc1.getDataset(openML);
				if (dataset1.length() > maxDatasetByteLength) {
					throw new Exception("Discard, dataset size is " + dataset1.length() + ">" + maxDatasetByteLength);
				}

				// keep r1
				runList1.add(r1);

			} catch (Exception e) {
				// discard r1
				System.err.println(e.getMessage());
			}
		}

		for (Run r2 : runTable2) {
			try {
				if (runList2.size() >= nbRunsForSpearman)
					break;

				// check r2
				// if missing eval discard
				if (!hasEval(auc2, r2.getRun_id())) {
					throw new Exception("Missing eval");
				}
				
				//if too many same task discard
				int nb=0;
				for (Run rl : runList2) {
					if(rl.getTask_id()==r2.getTask_id()) nb++;
				}
				if (nb>5) {
					throw new Exception("too many same task");
				}

				// check dataset
				int did2 = 0;
				boolean found = false;
				for (Task t : taskTable) {
					if (t.getTask_id() == r2.getTask_id()) {
						// if >500 features discard
						for (Quality q : t.getQualities()) {
							if (q.getName().equals("NumberOfFeatures") && Double.parseDouble(q.getValue()) > maxNbFeatures) {
								throw new Exception("Discard, too many features, " + Double.parseDouble(q.getValue()) + ">" + maxNbFeatures);
							}
						}
						did2 = t.getDid();
						found = true;
					}
				}
				if (!found) {
					for (Task t : downTaskTable) {
						if (t.getTask_id() == r2.getTask_id()) {
							// if >500 features discard
							for (Quality q : t.getQualities()) {
								if (q.getName().equals("NumberOfFeatures") && Double.parseDouble(q.getValue()) > maxNbFeatures) {
									throw new Exception("Discard, too many features, " + Double.parseDouble(q.getValue()) + ">" + maxNbFeatures);
								}
							}
							did2 = t.getDid();
							found = true;
						}
					}
					if (!found)
						throw new Exception("inactive task " + r2.getTask_id());
				}

				DataSetDescription desc2 = openML.dataGet(did2);
				// if unavailable or not arff discard
				if (!desc2.getVisibility().equals("public") || !desc2.getFormat().equals("ARFF")) {
					throw new Exception(desc2.getVisibility() + ", " + desc2.getFormat());
				}
				// actual download
				File dataset2 = desc2.getDataset(openML);
				if (dataset2.length() > maxDatasetByteLength) {
					throw new Exception("Discard, dataset size is " + dataset2.length() + ">" + maxDatasetByteLength);
				}

				// keep r2
				runList2.add(r2);

			} catch (Exception e) {
				// discard r2
				System.err.println(e.getMessage());
			}
		}

		// for each element of the cartesian product
		for (Run r1 : runList1) {
			for (Run r2 : runList2) {
				Controller.LOGGER.log(Level.FINEST, "Matching run " + r1.getRun_id() + "to run " + r2.getRun_id());

				// *******************************************
				// create couple dissim(datasets), dissim(evals)

				double dataDissim = dissimilarity.computeDataDissimilarity(r1.getTask_id(), r2.getTask_id());
				double evalDissim = Math.abs(getEval(auc1, r1.getRun_id()) - getEval(auc2, r2.getRun_id()));
				Controller.LOGGER.log(Level.FINEST, "Giving " + dataDissim + ", " + evalDissim);

				dualDissimilarities.add(new Pair(dataDissim, evalDissim));
			}
		}

		// spearman
		SpearmansCorrelation spearmansCorrelation = new SpearmansCorrelation();

		int size = dualDissimilarities.size();
		double[] dataDissims = new double[size];
		double[] evalDissims = new double[size];
		for (int i = 0; i < size; i++) {
			dataDissims[i] = dualDissimilarities.get(i).dataDissim;
			evalDissims[i] = dualDissimilarities.get(i).evalDissim;
		}
		
		double s = spearmansCorrelation.correlation(dataDissims, evalDissims);
		double t = s * Math.sqrt((size - 2) / (1 - s * s));
		double p = 1 - new TDistribution(size - 1).cumulativeProbability(t);
		double wfDissim = Math.log(2 / (s + 1)) / Math.log(2);

		Controller.LOGGER.log(Level.FINER, "Correlation between flow " + flowId1 + " and " + flowId2 + " of " + s + ", with p=" + p);
		Controller.LOGGER.log(Level.FINER, "************ Giving dissimilarity of " + wfDissim);

		return wfDissim;
	}

	private boolean hasEval(Evaluation[] evals, int runId) {
		for (Evaluation e : evals) {
			if (e.getRun_id() == runId)
				return true;
		}
		return false;
	}

	private Double getEval(Evaluation[] evals, int runId) {
		for (Evaluation e : evals) {
			if (e.getRun_id() == runId)
				return e.getValue();
		}
		return null;
	}

	private class Pair {
		public double dataDissim;
		public double evalDissim;

		public Pair(double dataDissim, double evalDissim) {
			this.dataDissim = dataDissim;
			this.evalDissim = evalDissim;
		}
		
		@Override
		public String toString() {
			return "Pair [dataDissim=" + dataDissim + ", evalDissim=" + evalDissim + "]";
		}
	}
}
