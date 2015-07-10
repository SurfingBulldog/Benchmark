package org.owasp.benchmark.score;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONObject;
import org.json.XML;
import org.owasp.benchmark.helpers.PropertiesManager;
import org.owasp.benchmark.helpers.Utils;
import org.owasp.benchmark.score.parsers.Reader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class WriteTime {
	public static void main(String[] args) throws Exception {
		// findbugs
		// mvn clean compile findbugs:findbugs -Dbuildtime.output.csv=true
		// -Dbuildtime.output.csv.file=classes\out.csv
		// pmd
		// mvn pmd:pmd -Dbuildtime.output.csv=true
		// -Dbuildtime.output.csv.file=classes\out.csv
		// findbugs
		// mvn clean compile -Pfindsecbugs -Dbuildtime.output.csv=true
		// -Dbuildtime.output.csv.file=classes\out.csv
		// sonar
		// mvn sonar:sonar -Dbuildtime.output.csv=true
		// -Dbuildtime.output.csv.file=classes\out.csv
		// rewrite results file name with times and version
		// String toolName = "sonar";
		String toolName = "";
		String csvToolName = "";
		if (args.length < 1) {
			System.out
					.println("Please provide the name of the tool.\n"
							+ "Currently supported: PMD (pmd), FindBugs (findbugs), FindSecBugs (findbugs) and SonarQube (sonar).");
		} else {
			toolName = args[0];
		}
		//System.out.println("Tool: " + toolName);

		PropertiesManager propM = new PropertiesManager();
		WriteFiles wf = new WriteFiles();
		if (toolName.contains("sonar")) { // We need to generate the results
			// file from the webService
			wf.writeSonarResults();
		}

		if (toolName.equals("findsecbugs")) {
			csvToolName = "findbugs";
		} else {
			csvToolName = toolName;
		}

		propM.saveProperty(toolName, wf.getToolTime(csvToolName));
		propM.saveProperty("benchmark-version", wf.getbenchmarkVersion()); //
		// propM.displayProperties();

		wf.deletePreviousResults(toolName, wf.getVersionNumber(toolName),
				propM.getProperty("benchmark-version", ""));

		wf.resultsFileName(toolName,
				propM.getProperty("benchmark-version", ""),
				propM.getProperty(toolName, ""), wf.getVersionNumber(toolName));
	}
}

class WriteFiles {
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final String CSV_TIMES_FILE = "out.csv";
	private static final String VERSION_FILE = "build.properties";
	private static final String SONAR_FILE = "target/sonarqube.xml";
	private static final String FINDBUGS_FILE = "target/findbugsXml.xml";
	private static final String PMD_FILE = "target/pmd.xml";

	public String getVersionNumber(String toolName) {
		try {
			File findbugsFile = new File(FINDBUGS_FILE);
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			InputSource is = null;
			Document doc = null;
			Node root = null;
			Reader reader = new Reader();

			switch (toolName) {
			case "findbugs":
				is = new InputSource(new FileInputStream(findbugsFile));
				doc = docBuilder.parse(is);
				root = doc.getDocumentElement();
				return reader.getAttributeValue("version", root);
			case "findsecbugs":
				return WriteFiles
						.getLine(new File("pom.xml"), "findsecbugs-plugin", true)
						.trim().replace("<version>", "")
						.replace("</version>", "");
			case "pmd":
				is = new InputSource(new FileInputStream(new File(PMD_FILE)));
				doc = docBuilder.parse(is);
				root = doc.getDocumentElement();
				return reader.getAttributeValue("version", root);
			case "sonar":
				return "TBD";
			}
		} catch (Exception e) {
			System.out.println("An error ocurred during results file parsing.");
		}
		return "";
	}

	public static String getLine(File file, String toFind, boolean nextLine) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line = "";
			while (line.equals("")) {
				line = br.readLine();
				if (line.contains(toFind)) {
					if (nextLine) {
						return br.readLine();
					} else {
						return line;
					}
				} else {
					line = "";
				}
			}
			// System.out.println(line.trim().replace(" ", ""));
		} catch (Exception e) {
			System.out.println("Error");
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				System.out.println("Can't close filereader for file: "
						+ file.getAbsolutePath() + " for some reason.");
				e.toString();
			}
		}
		return "";
	}

	public static void listPathFiles(String path) {
		String files;
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {
				files = listOfFiles[i].getName();
				System.out.println(files);
			}
		}
	}

	public void deletePreviousResults(String toolName, String toolVersion,
			String benchmarkVersion) {
		if (!toolName.equals("")) {
			File targetDir = new File("results/");
			if (targetDir.exists()) {
				File[] files = targetDir.listFiles();
				for (File file : files) {
					if (file.isFile()
							&& (file.getName().startsWith(
									"Benchmark_" + benchmarkVersion + "-"
											+ toolName + "-v" + toolVersion) && (file
									.getName().endsWith("xml")))) {
						file.delete();
					}
				}
				System.out
						.println("Deleted previously generated results files.");
			}
		}
	}

	public void resultsFileName(String tool, String benchmarkVersion,
			String times, String toolVersion) {
		String name = "results/Benchmark_" + benchmarkVersion + "-" + tool
				+ "-v" + toolVersion + "-" + times + ".xml";
		File file = null;

		File targetDir = new File("results/");
		if (!targetDir.exists()) {
			targetDir.mkdir();
		}

		// System.out.println("inside results file: "+tool);
		switch (tool) {
		case "findbugs":
		case "findsecbugs":
			file = new File(FINDBUGS_FILE);
			if (file.exists()) {
				file.renameTo(new File(name));
			}
			break;
		case "pmd":
			file = new File(PMD_FILE);
			if (file.exists()) {
				file.renameTo(new File(name));
			} else {
				System.out.println("PMD results file not found.");
			}
			break;
		case "sonar":
			file = new File(SONAR_FILE);
			if (file.exists()) {
				file.renameTo(new File(name));
			}
			break;
		}
	}

	public void writeSonarResults() {
		try {
			JSONObject json = new JSONObject(
					getSonarResults("http://localhost:9000"));
			String xml = XML.toString(json);
			java.io.FileWriter fw = new java.io.FileWriter(SONAR_FILE);
			fw.write(xml);
			fw.close();
		} catch (Exception e) {
			System.out
					.println("There was an error while writing SonarQube results.");
		}
	}

	public static String getSonarResults(String sonarURL) {
		StringBuffer response = new StringBuffer();
		try {
			String url = sonarURL + "/api/issues/search?resolved=false";
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			System.out
					.println("There was an error trying to retrieve SonarQube results.");
		}
		return response.toString();
	}

	public String getToolTime(String toolName) {
		String[] results = new String[3];
		String time = null;
		List<String> lines = Utils.getLinesFromFile(Utils.getFileFromClasspath(
				CSV_TIMES_FILE, this.getClass().getClassLoader()));
		for (String i : lines) {
			if (i.contains(toolName)) {
				results = i.split(";");
				time = results[2].replaceAll("\"", "");
				// System.out.println(time.split("\\.")[0]);
				return time.split("\\.")[0];
			}
		}
		return "";
	}

	public String getbenchmarkVersion() throws Exception {
		String[] results = new String[2];
		List<String> lines = Utils.getLinesFromFile(Utils.getFileFromClasspath(
				VERSION_FILE, this.getClass().getClassLoader()));
		for (String i : lines) {
			results = i.split("=");
			// System.out.println(results[1]);
			return results[1];
		}
		return "";

	}

}