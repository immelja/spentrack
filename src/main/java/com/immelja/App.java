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
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.joda.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import au.com.bytecode.opencsv.CSVReader;

public class App {
	private static Map<String, Transaction> currentMap = new TreeMap<String, Transaction>();
	private static Map<String, Transaction> newMap = new TreeMap<String, Transaction>();
	private static List<Transaction> transactions = new ArrayList<Transaction>();
	private static List<String> mappingProperties = new ArrayList<String>();
	private static List<String> term2ToDoLst = new ArrayList<String>();

	private static String applicationFolder = "./in/";
	private static String archiveFolder = "./archive/";
	private static String transactionFile = "standard";
	private static String json = ".json";
	
	private static int reportingPeriod = 201709;

	public static void main(String[] args) throws Exception {
		System.out.println("spentrack");
		scanDownloads();
		process();
		term2ToDo();
		analyse();
	}

	private static void avgToDate(Map<String, Transaction> map) {
		System.out.println("******* to date *******");
		Map<Integer, Float> periodBalances = new TreeMap<Integer, Float>();
		TreeSet<String> days = new TreeSet<String>();
		for (Transaction trn : map.values()) {
			days.add(trn.getDate());
		}
		int day = Integer.valueOf(days.last().substring(6, 8));
		System.out.println("latest day " + day);
		// LocalDate ld = new LocalDate();
		// System.out.println(ld);
		float bal = 0;
		for (Transaction trn : map.values()) {
			if (!periodBalances.containsKey(trn.getReportingPeriod())) {
				bal = 0;
			} else {
				bal = periodBalances.get(trn.getReportingPeriod()).longValue();
			}
			if (Integer.valueOf(trn.getDate().substring(6, 8)) <= day && trn.getAmount() < 0) {
				periodBalances.put(trn.getReportingPeriod(), bal + trn.getAmount());
			}
		}

		float sum = 0;
		int count = 0;

		for (Map.Entry<Integer, Float> entry : periodBalances.entrySet()) {
			count++;
			sum = sum + entry.getValue();
			System.out.println(entry);
		}
		System.out.println("Avg for day of month(" + day + ") " + sum / count);
	}

	private static void avgReportingPeriod(Map<String, Transaction> map) {
		Map<Integer, Float> periodBalances = new TreeMap<Integer, Float>();
		System.out.println("avgToDate map size " + map.size());
		LocalDate ld = new LocalDate();
		System.out.println(ld);
		float bal = 0;
		for (Transaction trn : map.values()) {
			if (!periodBalances.containsKey(trn.getReportingPeriod())) {
				bal = 0;
			} else {
				bal = periodBalances.get(trn.getReportingPeriod()).longValue();
			}
			periodBalances.put(trn.getReportingPeriod(), bal + trn.getAmount());
		}

		for (Map.Entry<Integer, Float> entry : periodBalances.entrySet()) {
			System.out.println(entry);
		}
	}

	private static void avgFinYear(Map<String, Transaction> map) {
		System.out.println("******* finyear *******");
		Map<Integer, Float> finYearBalances = new TreeMap<Integer, Float>();
		float bal = 0;
		for (Transaction trn : map.values()) {
			if (!finYearBalances.containsKey(trn.getFinYear())) {
				bal = 0;
			} else {
				bal = finYearBalances.get(trn.getFinYear()).longValue();
			}
			finYearBalances.put(trn.getFinYear(), bal + trn.getAmount());
		}

		for (Map.Entry<Integer, Float> entry : finYearBalances.entrySet()) {
			System.out.println(entry);
		}
	}
	
	private static void avgFinYearTerm(Map<String, Transaction> map,String income,String term) {
		System.out.println("******* finyear term ******* " + term);
		Map<Integer, Float> finYearBalances = new TreeMap<Integer, Float>();
		float bal = 0;
		for (Transaction trn : map.values()) {
			
//System.out.println(trn.toString());
		  if(trn.getIncome().equals(income)&&trn.getTerm().equals(term)) {
			if (!finYearBalances.containsKey(trn.getFinYear())) {
				bal = 0;
			} else {
				bal = finYearBalances.get(trn.getFinYear()).longValue();
			}
			finYearBalances.put(trn.getFinYear(), bal + trn.getAmount());
		  }
		}

		for (Map.Entry<Integer, Float> entry : finYearBalances.entrySet()) {
			System.out.println(entry);
		}
	}

	private static void analyse() {
		Map<String, Transaction> map = new TreeMap<String, Transaction>();
		map = loadJson(archiveFolder + transactionFile + json);
		avgReportingPeriod(map);
		avgFinYear(map);
		avgToDate(map);
		avgFinYearTerm(map,"INCOME","CENTRELINK");

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
		writeJson(newMap, archiveFolder + transactionFile, "CURRENT");
		System.out.println("after mapping size " + newMap.size());

		doTerm2();



		// term2ToDo(newMap);

	}

	private static void term2ToDo() throws IOException {
		Map<String, Transaction> map = new TreeMap<String, Transaction>();

		DateFormat df = new SimpleDateFormat("yyyyMM");

		map = loadJson(archiveFolder + transactionFile + json);
		// System.out.println(map.size());
		// Iterator<Entry<String, Transaction>> iter =
		// map.entrySet().iterator();
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
			// System.out.println(transaction);
			if (transaction.getReportingPeriod() == reportingPeriod
			&& transaction.getTerm2() == null
			&&transaction.getAmount() < 0
			) {
				bal = bal + transaction.getAmount();

				// System.out.println(transaction.toString());
				lines.add(transaction.getKey() 
						+ "|" + transaction.getTerm()
						+ "|" + transaction.getTerm2()
						+ "|" + transaction.getTerm3()
						);

			}
		}

		Files.write(file, lines, Charset.forName("UTF-8"));
		// System.out.println(bal);
	}

	private static void doTerm2() throws Exception {
		Map<String, Transaction> jaco = new TreeMap<String, Transaction>();
		Map<String, Transaction> hemla = new TreeMap<String, Transaction>();
		Map<String, Transaction> fixed = new TreeMap<String, Transaction>();

		Map<String, Transaction> current = new TreeMap<String, Transaction>();

		current = loadJson(archiveFolder + transactionFile + json);
		//System.out.println("json size before doTerm2  " + current.size());

		String term2ToDoFile = "./term2ToDo.csv";
		term2ToDoLst = readFile(term2ToDoFile);
		// System.out.println(term2ToDoLst);
		for (String prop : term2ToDoLst) {
			//System.out.println(prop);
			String[] parts = prop.split("\\|");
			//System.out.println("matched part 0? " + current.get(parts[0]).getTerm2() + " " + parts[1]);
			current.get(parts[0]).setTerm(parts[1]);
			current.get(parts[0]).setTerm2(parts[2]);
			current.get(parts[0]).setTerm3(parts[3]);
		}

		writeJson(current, archiveFolder + transactionFile, "CURRENT");

		Iterator<Entry<String, Transaction>> iter = current.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Transaction> tran = iter.next();
			if (tran.getValue().getReportingPeriod() == reportingPeriod) {

				if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("SPLIT")) {
					float splitAmount = tran.getValue().getAmount() / 2;
					String key = tran.getKey();
					Transaction t = tran.getValue();
					jaco.put(key, t);
					jaco.get(key).setAmount(splitAmount);
					hemla.put(key, t);
					hemla.get(key).setAmount(splitAmount);

				} else if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("JACO")) {
					jaco.put(tran.getKey(), tran.getValue());
				} else if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("HEMLA")) {
					hemla.put(tran.getKey(), tran.getValue());
				} else if (tran.getValue().getTerm2() != null && tran.getValue().getTerm2().equals("CURRENT")
						&& tran.getValue().getAmount() < 0) {
					fixed.put(tran.getKey(), tran.getValue());
				}
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
		for (String prop : mappingProperties) {
			String[] parts = prop.split(",");
			if (description.matches(".*" + parts[0] + ".*")) {
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
				//System.out.println(tran.toString());
			    tran.getValue().setTerm("UNMATCHED");
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
			// mapper.writeValue(new File(fileName + json), acc);
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			mapper.writeValue(new File(fileName + json), acc);
			//System.out.println(map.get("20170403_-200.0_WdlATMCBAATM99KINGSTAVIC302001AUS").getTerm2());
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return myEntries;
	}

	private static void addTransactions(File file) {
		for (String[] tran : load(file)) {
			Transaction newTran = new Transaction(tran[0], Float.valueOf(tran[1]), tran[2],
					tran[3].isEmpty() ? 0 : Float.valueOf(tran[3]), tran[4], null, null, null);
			transactions.add(newTran);
		}
		// System.out.println(transactions.size());
	}

	// read cnv
	private static Map<String, Transaction> loadCsv() {
		Map<String, Transaction> map = new TreeMap();
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
		Map<String, Transaction> map = new TreeMap();
		try {
			Account account = mapper.readValue(new File(fileName), Account.class);
			for (Transaction tran : account.getTransactions()) {
				map.put(tran.getKey(), tran);
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
