package com.immelja;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import au.com.bytecode.opencsv.CSVReader;

public class App {
	private static Map<String, Transaction> currentMap = new HashMap<String, Transaction>();
	private static Map<String, Transaction> newMap = new HashMap<String, Transaction>();
	private static List<Transaction> transactions = new ArrayList<Transaction>();
	private static List<String> mappingProperties = new ArrayList<String>();
	private static List<String> term2ToDoLst = new ArrayList<String>();

	private static String applicationFolder = "./in/";
	private static String archiveFolder = "./archive/";
	private static String transactionFile = "standard";
	private static String json = ".json";


	public static void main(String[] args) throws Exception {
		System.out.println("spentrack");
		scanDownloads();
		process();
	}

	private static void process() throws Exception {
		String mappingFileName = "./conf/mapping.properties";
		mappingProperties = readFile(mappingFileName);

		File directory = new File(archiveFolder);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File tnFileExist = new File(archiveFolder + transactionFile + json);
		if (tnFileExist.exists()) {
			currentMap = loadJson(archiveFolder + transactionFile + json);
			System.out.println("json size " + currentMap.size());
		}

		newMap = loadCsv();
		System.out.println("csv size " + newMap.size());

		newMap.putAll(currentMap);
		System.out.println("merged size " + newMap.size());

		doTerm(newMap);
		doTerm2();

		System.out.println("after mapping size " + newMap.size());

		writeJson(newMap, archiveFolder + transactionFile, "CURRENT");
		
		//term2ToDo(newMap);

	}

	private static void term2ToDo(Map<String, Transaction> map) throws IOException {

		DateFormat df = new SimpleDateFormat("yyyyMM");

		map = loadJson(archiveFolder + transactionFile + json);
		System.out.println(map.size());
		//Iterator<Entry<String, Transaction>> iter = map.entrySet().iterator();
		float bal = 0;
		Path file = Paths.get("term2ToDo.csv");
		List<String> lines = new ArrayList<String>();
		List<Transaction> transactions = new ArrayList<Transaction>(map.values());
		
		Collections.sort(transactions, new Comparator<Transaction>() {

			public int compare(Transaction t1, Transaction t2) {
				return (int) (t1.getAmount() - t2.getAmount());
			}
		});
		
		for (Transaction transaction : transactions) {
			System.out.println(transaction);
			if (transaction.getReportingPeriod() == 201704 
					//&& transaction.getTerm2() == null
			// &&transaction.getAmount() < 0
			) {
				bal = bal + transaction.getAmount();

				System.out.println(transaction.toString());
				lines.add(transaction.getKey() + "|CURRENT");

			}
		}
		
		Files.write(file, lines, Charset.forName("UTF-8"));
		System.out.println(bal);
	}

	private static void doTerm2() throws Exception {
		Map<String, Transaction> jaco = new HashMap<String, Transaction>();
		Map<String, Transaction> hemla = new HashMap<String, Transaction>();
		Map<String, Transaction> fixed = new HashMap<String, Transaction>();

        Map<String, Transaction> current = new HashMap<String, Transaction>();

        current = loadJson(archiveFolder + transactionFile + json);
		System.out.println("json size before doTerm2  " + current.size());

		String term2ToDoFile = "./term2ToDo.csv";
		term2ToDoLst = readFile(term2ToDoFile);
		System.out.println(term2ToDoLst);
		for (String prop : term2ToDoLst) {
			String[] parts = prop.split("\\|");
			current.get(parts[0]).setTerm2(parts[1]);
		}

		writeJson(newMap, archiveFolder + transactionFile, "CURRENT");

		Iterator<Entry<String, Transaction>> iter = current.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Transaction> tran = iter.next();

			if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("SPLIT")) {
			    System.out.println(tran.getValue().getDescription() + " " + tran.getValue().getAmount());
				float splitAmount = tran.getValue().getAmount() / 2;
				String key = tran.getKey();
				Transaction t = tran.getValue();
			    System.out.println(tran.getValue().getDescription() + " " + tran.getValue().getAmount());
				jaco.put(key, t);
			    System.out.println(tran.getValue().getDescription() + " " + tran.getValue().getAmount());
				jaco.get(key).setAmount(splitAmount);
			    System.out.println(tran.getValue().getDescription() + " " + tran.getValue().getAmount());
				hemla.put(key, t);
				hemla.get(key).setAmount(splitAmount);

			} else if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("JACO")) {
				jaco.put(tran.getKey(), tran.getValue());
			} else if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("HEMLA")) {
				hemla.put(tran.getKey(), tran.getValue());
			} else if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("CURRENT") && tran.getValue().getAmount() < 0) {
				fixed.put(tran.getKey(), tran.getValue());
			}

		}
		writeJson(jaco, "./json/jaco", "JACO");
		writeJson(hemla, "./json/hemla", "HEMLA");
		writeJson(fixed, "./json/fixed", "FIXED");

	}

	private static List<String> readFile(String filename) throws Exception {
		String line = null;
		List<String> records = new ArrayList<String>();

		File file = new File(filename);
		if (file.exists()) {

			BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
			while ((line = bufferedReader.readLine()) != null) {
				records.add(line);
			}

			// close the BufferedReader when we're done
			bufferedReader.close();
		}
		return records;
	}

	private static String getMatch(String description) {
		// System.out.println(description);

		for (String prop : mappingProperties) {
			// System.out.println(prop);
			String[] parts = prop.split(",");
			// System.out.println(parts[0] + " " + parts[1]);
			if (description.matches(".*" + parts[0] + ".*")) {
				// System.out.println(parts[1]+"******************
				// "+description);
				return parts[1];
			}
		}
		return null;
	}

	private static void doTerm(Map<String, Transaction> map) {
		map.remove("1477400400000_-8100.0_HomeLoanPymtCommBankapp");
		map.remove("1479906000000_-670.0_HomeLoanPymtCommBankapp");

		Iterator<Entry<String, Transaction>> iter = map.entrySet().iterator();
		Set<String> set = new HashSet();
		while (iter.hasNext()) {
			Entry<String, Transaction> tran = iter.next();
			if ("Exclude".equalsIgnoreCase(tran.getValue().getCategory())) {
				iter.remove();
			}

			if ("EXCLUDE".equalsIgnoreCase(tran.getValue().getTerm())) {
				iter.remove();
			}

			if ("Home Loan Pymt CommBank app".equalsIgnoreCase(tran.getValue().getDescription())) {
				System.out.println(tran.getKey());
			}

			tran.getValue().setTerm(getMatch(tran.getValue().getDescription()));

			if (tran.getValue().getTerm() == null)
				set.add(tran.getValue().getDescription());
		}

		for (String setItem : set) {
			System.out.println(setItem + ",UNMATCHED");
		}
		System.out.println("UNMATCHED:: " + set.size());
	}

	private static void writeJson(Map<String, Transaction> map, String fileName, String account) {
		Account acc = new Account();
		acc.setType(account);
		acc.setTransactions(new ArrayList<Transaction>(map.values()));
		for (Transaction tran : acc.getTransactions()) {
			acc.setBalance(acc.getBalance() + tran.getAmount());
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
            mapper.writeValue(new File(fileName + json), acc);
            //DateFormat df = new SimpleDateFormat("yyyyMMdd");
            //mapper.setDateFormat(df);
            //mapper.writeValue(new File(fileName + "Date" + json), acc);
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(fileName + "Formatted" + json), acc);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<String[]> load(File file) {
		List<String[]> myEntries = null;
		try {
			CSVReader reader = new CSVReader(new FileReader(file));
			myEntries = reader.readAll();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return myEntries;
	}

	private static void addTransactions(File file) {
		for (String[] tran : load(file)) {
			Transaction newTran = new Transaction(tran[0], Float.valueOf(tran[1]), tran[2],
					tran[3].isEmpty() ? 0 : Float.valueOf(tran[3]), tran[4], null, null);
			transactions.add(newTran);
		}
		System.out.println(transactions.size());
	}

	// read cnv
	private static Map<String, Transaction> loadCsv() {
		Map<String, Transaction> map = new HashMap();
		// File f = new File("C:\\Users\\immeljac\\Downloads\\in");
		File f = new File(applicationFolder);
		for (File file : f.listFiles()) {
			addTransactions(file);
		}
		for (Transaction tran : transactions) {
			map.put(tran.getKey(), tran);
		}
		return map;
	}

	static Map<String, Transaction> loadJson(String fileName) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Transaction> map = new HashMap();
		try {
			Account account = mapper.readValue(new File(fileName), Account.class);
			for (Transaction tran : account.getTransactions()) {
				map.put(tran.getKey(), tran);
			}
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	private static String fileName(File f) throws IOException, ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMdd");
		CSVReader reader = new CSVReader(new FileReader(f));
		List<String[]> entryList = reader.readAll();
		String[] first = (String[]) entryList.get(0);
		try {
			Date date = formatter.parse(entryList.get(0)[0]);
		} catch (Exception e) {
			reader.close();
			return null;
		}
		boolean credit = !first[3].equals("");

		String[] last = (String[]) entryList.get(entryList.size() - 1);
		TreeSet<Date> dateSet = new TreeSet<Date>();
		for (String[] entry : entryList) {
			dateSet.add(formatter.parse(entry[0]));
		}

		reader.close();
		return formatter2.format(dateSet.first()) + "_" + formatter2.format(dateSet.last())
				+ (!credit ? "credit" : "savings") + entryList.size() + ".csv";
	}

	/**
	 * Search through Downloads folder for CBA statement files. Move to ./in
	 * folder
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void scanDownloads() throws IOException, ParseException {
		System.out.println("user home " + System.getProperty("user.home"));
		int count = 0;

		File directory = new File(applicationFolder);
		if (!directory.exists()) {
			directory.mkdir();
		}
		Path movefrom = FileSystems.getDefault().getPath(System.getProperty("user.home") + "/Downloads");

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(movefrom, "*.csv")) {
			for (Path file : stream) {
				System.out.println(file.toFile() + " -> " + applicationFolder + fileName(file.toFile()));
				try {
					Files.move(file, FileSystems.getDefault().getPath(applicationFolder + fileName(file.toFile())),
							StandardCopyOption.REPLACE_EXISTING);
					count++;
				} catch (IOException e) {
					System.err.println(e);
				}
			}
		} catch (IOException | DirectoryIteratorException x) {
			System.err.println(x);
		}

		System.out.println(count + " files moved from Downloads -> application folder");

	}
}
