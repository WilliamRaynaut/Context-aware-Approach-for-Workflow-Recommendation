package wfDissim;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.*;

import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.RunList.Run;

import main.Controller;

public class TresholdExplorer {

	public void exploreTreshold(OpenmlConnector openML) throws Exception {

		// ArrayList<Integer> bayes = new ArrayList<Integer>(Arrays.asList(new Integer[] { 58, 67, 121, 441, 548, 2318, 3905, 4793, 7026, 7240 }));
		// ArrayList<Integer> forest = new ArrayList<Integer>(Arrays.asList(new Integer[] { 65, 165, 506, 1248, 1822, 2332, 2629, 3434, 5490, 5723 }));
		// ArrayList<Integer> svm = new ArrayList<Integer>(Arrays.asList(new Integer[] { 1172, 1219, 2312, 2336, 4828, 4835, 5728, 6269, 4006, 5527 }));
		// ArrayList<Integer> tree = new ArrayList<Integer>(Arrays.asList(new Integer[] { 61, 64, 1080, 1087, 1089, 1091, 4834, 7253, 6747, 2264 }));
		// ArrayList<Integer> ada = new ArrayList<Integer>(Arrays.asList(new Integer[] { 75, 207, 205, 214, 222, 6970, 1818, 616, 418, 7116 }));

//		ArrayList<Integer> bayes = new ArrayList<Integer>(Arrays.asList(new Integer[] { 58, 380, 1154, 1745, 3489 }));
//		ArrayList<Integer> forest = new ArrayList<Integer>(Arrays.asList(new Integer[] { 65, 391, 1079, 1726, 5923 }));
//		ArrayList<Integer> svm = new ArrayList<Integer>(Arrays.asList(new Integer[] { 2336, 2584, 3353, 3361, 3676 }));
//		ArrayList<Integer> tree = new ArrayList<Integer>(Arrays.asList(new Integer[] { 61, 64, 1077, 1078, 1724 }));
//		ArrayList<Integer> ada = new ArrayList<Integer>(Arrays.asList(new Integer[] { 75, 376, 1182, 2267, 1195 }));

		ArrayList<ArrayList<Integer>> uglys = new ArrayList<ArrayList<Integer>>();
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 58, 65, 1172, 61, 75 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 67, 165, 1219, 64, 207 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 121, 506, 2312, 1080, 205 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 441, 1248, 2336, 1087, 214 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 548, 1822, 4828, 1089, 222 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 2318, 2332, 4835, 1091, 6970 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 3905, 2629, 5728, 4834, 1818 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 4793, 3434, 6269, 7253, 616 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 7026, 5490, 4006, 6747, 418 })));
		uglys.add(new ArrayList<Integer>(Arrays.asList(new Integer[] { 7240, 5723, 5527, 2264, 7116 })));

		WorkflowDissimilarity workflowDissimilarity = new WorkflowDissimilarity();
		DescriptiveStatistics d;

//		d = workflowDissimilarity.computeDissimilarity(bayes, openML);
//		System.out.println("bayes " + d.getMean() + ", " + d.getStandardDeviation());
//		Controller.LOGGER.log(Level.FINE, "bayes " + d.getMean() + ", " + d.getStandardDeviation());
//
//		d = workflowDissimilarity.computeDissimilarity(forest, openML);
//		System.out.println("forest " + d.getMean() + ", " + d.getStandardDeviation());
//		Controller.LOGGER.log(Level.FINE, "forest " + d.getMean() + ", " + d.getStandardDeviation());
//
//		d = workflowDissimilarity.computeDissimilarity(svm, openML);
//		System.out.println("svm " + d.getMean() + ", " + d.getStandardDeviation());
//		Controller.LOGGER.log(Level.FINE, "svm " + d.getMean() + ", " + d.getStandardDeviation());
//
//		d = workflowDissimilarity.computeDissimilarity(tree, openML);
//		System.out.println("tree " + d.getMean() + ", " + d.getStandardDeviation());
//		Controller.LOGGER.log(Level.FINE, "tree " + d.getMean() + ", " + d.getStandardDeviation());
//
//		d = workflowDissimilarity.computeDissimilarity(ada, openML);
//		System.out.println("ada " + d.getMean() + ", " + d.getStandardDeviation());
//		Controller.LOGGER.log(Level.FINE, "ada " + d.getMean() + ", " + d.getStandardDeviation());

		for (int i = 0; i < uglys.size(); i++) {
			d = workflowDissimilarity.computeDissimilarity(uglys.get(i), openML);
			Controller.LOGGER.log(Level.FINE, "ugly" + i + " " + d.getMean() + ", " + d.getStandardDeviation());
		}

	}

	public void makeFlowSet(OpenmlConnector openML, String pattern) throws Exception {

		URL url = new URL("https://www.openml.org/api/v1/json/flow/list?api_key=c9970378e3526821e0fa2131b9533f47");
		Scanner scan = new Scanner(url.openStream());
		String str = new String();
		while (scan.hasNext())
			str += scan.nextLine();
		scan.close();

		PrintWriter writer = new PrintWriter(new File(Controller.mainDir, pattern + ".txt"), "UTF-8");
		JSONObject obj = new JSONObject(str);
		JSONArray arr = obj.getJSONObject("flows").getJSONArray("flow");
		for (int i = 0; i < arr.length(); i++) {
			int id = arr.getJSONObject(i).getInt("id");
			String name = arr.getJSONObject(i).getString("name").toLowerCase();

			if (name.contains(pattern)) {
				System.out.println(id + ", " + name);
				try {
					// count runs
					HashMap<String, List<Integer>> filter = new HashMap<String, List<Integer>>();
					ArrayList<Integer> tmp = new ArrayList<Integer>();
					tmp.add(id);
					filter.put("flow", tmp);
					Run[] runForFlow = openML.runList(filter, 500, 0).getRuns();
					ArrayList<Integer> runCount = new ArrayList<Integer>();
					for (Run r1 : runForFlow) {
						if (!runCount.contains(r1.getRun_id())) {
							if (openML.taskGet(r1.getTask_id()).getTask_type_id() == 1)
								runCount.add(r1.getRun_id());
						}
						if (runCount.size() > 30) {
							writer.println(id + ", " + name);
							break;
						}
					}
				} catch (Exception e) {
				}

			}
		}
		writer.close();

	}

}
