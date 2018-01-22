package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.EvaluationList.Evaluation;
import org.openml.apiconnector.xml.RunList.Run;
import org.openml.apiconnector.xml.Tasks.Task;
import org.openml.apiconnector.xml.Tasks.Task.Input;
import org.openml.apiconnector.xml.Tasks.Task.Quality;

public class DataCollector {

	public static final ArrayList<Integer> userIds = new ArrayList<Integer>(Arrays.asList(new Integer[] { 268, 2258, 932, 791, 2026, 259, 793, 3128, 1355, 2540,
			2555, 763, 797, 428, 812, 1103, 1776, 258, 753, 2261, 3153, 889, 266, 1769, 255, 815, 789, 2517, 790, 2436, 1972, 3362, 1056, 2442, 260, 794, 795,
			2373, 1968, 788, 286, 2593, 1938, 1992, 1928, 760, 2654, 2511, 2239, 2617, 274, 980, 3433, 1982, 1991, 2038, 1976, 2507, 1931, 2344, 427, 1959,
			2767, 272, 1072, 780, 1077, 1969, 901, 1045, 1046, 261, 1163, 1994, 2781, 1006, 2608, 1949, 271, 1929, 263, 280, 1932, 2752, 257, 2768, 1993, 1952,
			1955, 262, 2872, 2572, 645, 2441, 269, 267, 820, 2481, 1945, 2482, 2755, 835, 2039, 2568, 2479, 2252, 1943, 2432, 2811, 1145, 2451, 2819, 27, 287,
			1977, 1933, 2431, 2902, 3375, 1964, 265, 2505, 1066, 2748, 1995, 2888, 2900, 1989, 1942, 507, 1939, 762, 2081, 1997, 2244, 2729, 1937, 86, 1954,
			2337, 900, 2793, 2069, 1983, 2749, 2496, 225, 3552, 417, 1184, 1950, 2747, 2666, 1990, 1973, 2818, 531, 2792, 245, 3345, 2514, 2740, 1140, 1975,
			3378, 2807, 570, 2523, 2806, 1960, 1935, 1961, 1948, 1956, 1981, 1967, 2471, 270, 2001, 1035, 2010, 2713, 1988, 1936, 568, 3886, 1947, 2809, 2746,
			1124, 2742, 1946, 239, 2779, 1912, 1927, 1478, 2745, 869, 2775, 2717, 2735, 1738, 3002, 212, 576, 1965, 3546, 482, 1129, 2993, 726, 1290, 1055, 505,
			2497, 1336, 302, 1962, 842, 1065, 974, 2334, 350, 2273, 975, 383, 796, 1070, 2466, 2825, 874, 1164, 1980, 1491, 2557, 2867, 264, 2787, 894, 1198,
			2750, 3475, 3312, 3506, 284, 3535, 1970, 1271, 2766, 3348, 2014, 2521, 2260, 2609, 759, 2265, 2506, 1372, 3398, 870, 1940, 1930, 2276, 3156, 1953,
			2277, 806, 2102, 1944, 2278, 2513, 3277, 2280, 282, 2587, 2491, 827, 2317, 1963, 2445, 3152, 2543, 2336, 1966, 3211, 3462, 571, 281, 2583, 443,
			1934, 1265, 2295, 2515, 29, 2302, 2315, 2332, 2773, 2040, 3420, 569, 2508, 275, 1996, 787, 480 }));

	private final int step = 200;
	private final int maxHumanRuns = 20;
	private final int maxNbFeatures = 1000;
	private final int maxDatasetByteLength = 50000000;
	private final int minRunsPerFlow = 50;

	@SuppressWarnings("unchecked")
	public void descRuns(OpenmlConnector openML) throws Exception {
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load("goodRuns");
		for (int rid : goodRuns) {
			System.out.println("run " + rid + " : " + openML.runGet(rid).getTask_id() + ", " + openML.runGet(rid).getUploader() + ", "
					+ openML.runGet(rid).getFlow_id() + ", " + openML.runGet(rid).getFlow_name());

		}
	}

	@SuppressWarnings("unchecked")
	public void tagStuff(OpenmlConnector openML) throws Exception {
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load("goodRuns");
		for (int rid : goodRuns) {
			org.openml.apiconnector.xml.Run r = openML.runGet(rid);
			try {
				openML.runTag(rid, "study_73");
			} catch (Exception e) {
			}
			try {
				openML.flowTag(r.getFlow_id(), "study_73");
			} catch (Exception e) {
			}
			try {
				openML.taskTag(r.getTask_id(), "study_73");
			} catch (Exception e) {
			}
		}
	}

	public void findRuns(OpenmlConnector openML) throws Exception {

		// get classif tasks
		Task[] taskTable = openML.taskList("active", 1).getTask();
		ArrayList<Integer> taskIds = new ArrayList<Integer>();
		for (Task t : taskTable) {
			if (!taskIds.contains(t.getTask_id()))
				taskIds.add(t.getTask_id());
		}

		// inits
		ArrayList<Integer> coveredFlows = new ArrayList<Integer>();
		ArrayList<Integer> coveredTasks = new ArrayList<Integer>();
		ArrayList<Integer> goodRuns = new ArrayList<Integer>();

		// main loop
		for (int taskId : taskIds) {
			try {
				Controller.LOGGER.log(Level.FINE, "Getting runs for task " + taskId);

				int current = 0;
				boolean foundRun = false;
				while (!foundRun) {

					// get runs on task by legit users
					HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
					ArrayList<Integer> tmp = new ArrayList<Integer>();
					tmp.add(taskId);
					filters.put("task", tmp);
					filters.put("uploader", userIds);

					Run[] runTable = openML.runList(filters, step, current).getRuns();

					ArrayList<Integer> runIds = new ArrayList<Integer>();
					for (Run r : runTable) {
						if (!runIds.contains(r.getRun_id()))
							runIds.add(r.getRun_id());
					}

					// get evaluations
					Evaluation[] auc = openML.evaluationList(runIds, null, null, "area_under_roc_curve").getEvaluations();
					Evaluation[] kappa = openML.evaluationList(runIds, null, null, "kappa").getEvaluations();
					Evaluation[] info = openML.evaluationList(runIds, null, null, "kb_relative_information_score").getEvaluations();
					Evaluation[] acc = openML.evaluationList(runIds, null, null, "predictive_accuracy").getEvaluations();

					// check each run
					for (Run r : runTable) {
						try {
							Controller.LOGGER.log(Level.FINER, "Checking run " + r.getRun_id());

							// if task covered get out
							if (coveredTasks.contains(r.getTask_id())) {
								break;
							}

							// if flow covered discard
							if (coveredFlows.contains(r.getFlow_id())) {
								throw new Exception("Flow covered");
							}

							// if missing eval discard
							if ((!hasEval(auc, r.getRun_id())) || (!hasEval(kappa, r.getRun_id())) || (!hasEval(info, r.getRun_id()))
									|| (!hasEval(acc, r.getRun_id()))) {
								throw new Exception("Missing eval");
							}

							// search runs with same user-task
							filters = new HashMap<String, List<Integer>>();
							ArrayList<Integer> tmp1 = new ArrayList<Integer>();
							tmp1.add(r.getTask_id());
							filters.put("task", tmp1);
							ArrayList<Integer> tmp2 = new ArrayList<Integer>();
							tmp2.add(r.getUploader());
							filters.put("uploader", tmp2);
							Run[] sameRuns = openML.runList(filters, maxHumanRuns * 10, null).getRuns();
							// count runs
							ArrayList<Integer> runCount = new ArrayList<Integer>();
							for (Run r1 : sameRuns) {
								if (!runCount.contains(r1.getRun_id()))
									runCount.add(r1.getRun_id());
							}
							// if at least Y found discard
							if (runCount.size() > maxHumanRuns) {
								throw new Exception("Discard, not human, " + runCount.size() + ">" + maxHumanRuns);
							}
							Controller.LOGGER.log(Level.FINER, "Is Human");

							// count runs with flow
							HashMap<String, List<Integer>> filter = new HashMap<String, List<Integer>>();
							ArrayList<Integer> tmp3 = new ArrayList<Integer>();
							tmp3.add(r.getFlow_id());
							filter.put("flow", tmp3);
							Run[] runForFlow = openML.runList(filter, minRunsPerFlow * 10, 0).getRuns();
							// count runs
							runCount = new ArrayList<Integer>();
							for (Run r1 : runForFlow) {
								if (!runCount.contains(r1.getRun_id()))
									runCount.add(r1.getRun_id());
							}
							// if less than X runs, discard
							if (runCount.size() < minRunsPerFlow) {
								throw new Exception("Discard, not enough runs, " + runCount.size() + "<" + minRunsPerFlow);
							}
							Controller.LOGGER.log(Level.FINER, "Is in DB");

							// check dataset
							int did = 0;
							for (Task t : taskTable) {
								if (t.getTask_id() == r.getTask_id()) {
									// if >500 features discard
									for (Quality q : t.getQualities()) {
										if (q.getName().equals("NumberOfFeatures") && Double.parseDouble(q.getValue()) > maxNbFeatures) {
											throw new Exception("Discard, too many features, " + Double.parseDouble(q.getValue()) + ">" + maxNbFeatures);
										}
									}
									did = t.getDid();
									// check that eval crit is one of ours
									for (Input i : t.getInputs()) {
										if (i.getName().equals("evaluation_measures")) {
											if (!(i.getValue().equals("area_under_roc_curve") || i.getValue().equals("kappa")
													|| i.getValue().equals("kb_relative_information_score") || i.getValue().equals("predictive_accuracy")))
												throw new Exception("Discard, evaluation_measures is " + i.getValue());
										}
									}
								}
							}
							DataSetDescription desc = openML.dataGet(did);
							// if unavailable or not arff discard
							if (!desc.getVisibility().equals("public") || !desc.getFormat().equals("ARFF")) {
								throw new Exception(desc.getVisibility() + ", " + desc.getFormat());
							}
							// actual download
							File dataset = desc.getDataset(openML);
							if (dataset.length() > maxDatasetByteLength) {
								throw new Exception("Discard, dataset size is " + dataset.length() + ">" + maxDatasetByteLength);
							}
							Controller.LOGGER.log(Level.FINER, "Has dataset");

							// if all tests pass, keep run
							foundRun = true;
							goodRuns.add(r.getRun_id());
							coveredTasks.add(r.getTask_id());
							coveredFlows.add(r.getFlow_id());

							Controller.LOGGER.log(Level.FINE, "**********   New run   ***********");
							Controller.LOGGER.log(Level.FINE, goodRuns.toString());
							Controller.LOGGER.log(Level.FINE, coveredTasks.toString());

							Controller.save(goodRuns, "goodRuns");
							Controller.save(coveredTasks, "coveredTasks");
							Controller.save(coveredFlows, "coveredFlows");

						} catch (Exception e) {
							// discarding r
							Controller.LOGGER.log(Level.FINER, e.getMessage());
						}
					}

					current += step;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// trim some runs ?

	}

	@SuppressWarnings("unchecked")
	public void moreRuns(OpenmlConnector openML) throws Exception {

		// inits
		ArrayList<Integer> coveredFlows = (ArrayList<Integer>) Controller.load("coveredFlows");
		ArrayList<Integer> coveredTasks = (ArrayList<Integer>) Controller.load("coveredTasks");
		ArrayList<Integer> goodRuns = (ArrayList<Integer>) Controller.load("goodRuns");
		ArrayList<Integer> reCoveredFlows = new ArrayList<Integer>();

		// get classif tasks
		Task[] taskTable = openML.taskList("active", 1).getTask();
		ArrayList<Integer> taskIds = new ArrayList<Integer>();
		for (Task t : taskTable) {
			if (!taskIds.contains(t.getTask_id()) && !coveredTasks.contains(t.getTask_id()))
				taskIds.add(t.getTask_id());
		}

		// main loop
		for (int taskId : taskIds) {
			try {
				Controller.LOGGER.log(Level.FINE, "Getting runs for task " + taskId);

				int current = 0;
				boolean foundRun = false;
				while (!foundRun) {

					// get runs on task by legit users
					HashMap<String, List<Integer>> filters = new HashMap<String, List<Integer>>();
					ArrayList<Integer> tmp = new ArrayList<Integer>();
					tmp.add(taskId);
					filters.put("task", tmp);
					filters.put("uploader", userIds);

					Run[] runTable = openML.runList(filters, step, current).getRuns();

					ArrayList<Integer> runIds = new ArrayList<Integer>();
					for (Run r : runTable) {
						if (!runIds.contains(r.getRun_id()))
							runIds.add(r.getRun_id());
					}

					// get evaluations
					Evaluation[] auc = openML.evaluationList(runIds, null, null, "area_under_roc_curve").getEvaluations();
					Evaluation[] kappa = openML.evaluationList(runIds, null, null, "kappa").getEvaluations();
					Evaluation[] info = openML.evaluationList(runIds, null, null, "kb_relative_information_score").getEvaluations();
					Evaluation[] acc = openML.evaluationList(runIds, null, null, "predictive_accuracy").getEvaluations();

					// check each run
					for (Run r : runTable) {
						try {
							Controller.LOGGER.log(Level.FINER, "Checking run " + r.getRun_id());

							// if task covered get out
							if (coveredTasks.contains(r.getTask_id())) {
								break;
							}

							// if flow covered discard
							if (coveredFlows.contains(r.getFlow_id())) {
								if (reCoveredFlows.contains(r.getFlow_id())) {
									throw new Exception("Flow covered");
								}
							}

							// if missing eval discard
							if ((!hasEval(auc, r.getRun_id())) || (!hasEval(kappa, r.getRun_id())) || (!hasEval(info, r.getRun_id()))
									|| (!hasEval(acc, r.getRun_id()))) {
								throw new Exception("Missing eval");
							}

							// search runs with same user-task
							filters = new HashMap<String, List<Integer>>();
							ArrayList<Integer> tmp1 = new ArrayList<Integer>();
							tmp1.add(r.getTask_id());
							filters.put("task", tmp1);
							ArrayList<Integer> tmp2 = new ArrayList<Integer>();
							tmp2.add(r.getUploader());
							filters.put("uploader", tmp2);
							Run[] sameRuns = openML.runList(filters, maxHumanRuns * 10, null).getRuns();
							// count runs
							ArrayList<Integer> runCount = new ArrayList<Integer>();
							for (Run r1 : sameRuns) {
								if (!runCount.contains(r1.getRun_id()))
									runCount.add(r1.getRun_id());
							}
							// if at least Y found discard
							if (runCount.size() > maxHumanRuns) {
								throw new Exception("Discard, not human, " + runCount.size() + ">" + maxHumanRuns);
							}
							Controller.LOGGER.log(Level.FINER, "Is Human");

							// count runs with flow
							HashMap<String, List<Integer>> filter = new HashMap<String, List<Integer>>();
							ArrayList<Integer> tmp3 = new ArrayList<Integer>();
							tmp3.add(r.getFlow_id());
							filter.put("flow", tmp3);
							Run[] runForFlow = openML.runList(filter, minRunsPerFlow * 10, 0).getRuns();
							// count runs
							runCount = new ArrayList<Integer>();
							for (Run r1 : runForFlow) {
								if (!runCount.contains(r1.getRun_id()))
									runCount.add(r1.getRun_id());
							}
							// if less than X runs, discard
							if (runCount.size() < minRunsPerFlow) {
								throw new Exception("Discard, not enough runs, " + runCount.size() + "<" + minRunsPerFlow);
							}
							Controller.LOGGER.log(Level.FINER, "Is in DB");

							// check dataset
							int did = 0;
							for (Task t : taskTable) {
								if (t.getTask_id() == r.getTask_id()) {
									// if >500 features discard
									for (Quality q : t.getQualities()) {
										if (q.getName().equals("NumberOfFeatures") && Double.parseDouble(q.getValue()) > maxNbFeatures) {
											throw new Exception("Discard, too many features, " + Double.parseDouble(q.getValue()) + ">" + maxNbFeatures);
										}
									}
									did = t.getDid();
									// check that eval crit is one of ours
									for (Input i : t.getInputs()) {
										if (i.getName().equals("evaluation_measures")) {
											if (!(i.getValue().equals("area_under_roc_curve") || i.getValue().equals("kappa")
													|| i.getValue().equals("kb_relative_information_score") || i.getValue().equals("predictive_accuracy")))
												throw new Exception("Discard, evaluation_measures is " + i.getValue());
										}
									}
								}
							}
							DataSetDescription desc = openML.dataGet(did);
							// if unavailable or not arff discard
							if (!desc.getVisibility().equals("public") || !desc.getFormat().equals("ARFF")) {
								throw new Exception(desc.getVisibility() + ", " + desc.getFormat());
							}
							// actual download
							File dataset = desc.getDataset(openML);
							if (dataset.length() > maxDatasetByteLength) {
								throw new Exception("Discard, dataset size is " + dataset.length() + ">" + maxDatasetByteLength);
							}
							Controller.LOGGER.log(Level.FINER, "Has dataset");

							// if all tests pass, keep run
							foundRun = true;
							goodRuns.add(r.getRun_id());
							coveredTasks.add(r.getTask_id());
							if (coveredFlows.contains(r.getFlow_id())) {
								reCoveredFlows.add(r.getFlow_id());
							} else {
								coveredFlows.add(r.getFlow_id());
							}

							Controller.LOGGER.log(Level.FINE, "**********   New run   ***********");
							Controller.LOGGER.log(Level.FINE, goodRuns.toString());
							Controller.LOGGER.log(Level.FINE, coveredTasks.toString());

							Controller.save(goodRuns, "goodRuns");
							Controller.save(coveredTasks, "coveredTasks");
							Controller.save(coveredFlows, "coveredFlows");

						} catch (Exception e) {
							// discarding r
							Controller.LOGGER.log(Level.FINER, e.getMessage());
						}
					}

					current += step;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private boolean hasEval(Evaluation[] evals, int runId) {
		for (Evaluation e : evals) {
			if (e.getRun_id() == runId)
				return true;
		}
		return false;
	}

}
