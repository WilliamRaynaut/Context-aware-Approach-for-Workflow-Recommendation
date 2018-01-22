package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.openml.apiconnector.io.OpenmlConnector;

import wfDissim.TresholdExplorer;

public class Controller {

	public static Logger LOGGER = Logger.getLogger(Controller.class.getName());

	// main files
	public static final File mainDir = new File("D:\\Work\\RecoExp");
	// public static final File mainDir = new File("C:\\Users\\admin\\Dropbox\\Exp\\Recoexp");
	public static final File logDir = new File(mainDir, "log\\");
	public static final File datasetsDir = new File(mainDir, "datasets\\");
	public static final File dmfsDir = new File(mainDir, "dmfs\\");

	public static final String apiKey = "ad6244a6f01a5c9fc4985a0875b30b97";
	public static OpenmlConnector openML = new OpenmlConnector(apiKey);

	public static void main(String[] args) throws Exception {
		// logging
		Locale.setDefault(Locale.ENGLISH);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		FileHandler fh = new FileHandler(new File(Controller.logDir, "log." + sdf.format(timestamp) + ".txt").getAbsolutePath());
		fh.setFormatter(new SimpleFormatter());
		LOGGER.addHandler(fh);

		// verbosity
		LOGGER.setLevel(Level.FINE);
		openML.setVerboseLevel(0);
		Controller.LOGGER.log(Level.FINE, "Main Starting");

		// main
		// DataCollector dataCollector = new DataCollector();
		// dataCollector.findRuns(openML);
		// dataCollector.moreRuns(openML);
		// dataCollector.descRuns(openML);
		// dataCollector.tagStuff(openML);

		//DissimMats dissimMats = new DissimMats();
		//dissimMats.allSplits(openML);

		CrossValidation crossValidation = new CrossValidation();
		crossValidation.computeEval("wffarthestRuns", openML);
		//
		// System.out.println("");

		// Baseline baseline = new Baseline();
		// baseline.alwaysBest(openML);

		// TresholdExplorer tresholdExplorer = new TresholdExplorer();
		// tresholdExplorer.makeFlowSet(openML, "bayes");
		// tresholdExplorer.makeFlowSet(openML, "forest");
		// tresholdExplorer.makeFlowSet(openML, "svm");
		// tresholdExplorer.makeFlowSet(openML, "tree");
		// tresholdExplorer.makeFlowSet(openML, "ada");
		// tresholdExplorer.exploreTreshold(openML);
	}

	public static void save(Object obj, String filename) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(mainDir, filename)));
		oos.writeObject(obj);
		oos.close();
	}

	public static Object load(String filename) throws Exception {
		ObjectInputStream ois;
		ois = new ObjectInputStream(new FileInputStream(new File(mainDir, filename)));
		Object res = ois.readObject();
		ois.close();
		return res;
	}

}
