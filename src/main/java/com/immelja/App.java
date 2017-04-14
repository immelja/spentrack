package com.immelja;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private static String applicationFolder = "./in/";
	private static String archiveFolder = "./archive/";
	private static String transactionFile = "standard.json";

	public static void main(String[] args) throws IOException, ParseException {
		System.out.println("spentrack");
		scanDownloads();
		process();
	}

	private static void process() {
		File directory = new File(archiveFolder);
		if (!directory.exists()) {
			directory.mkdir();
		}
		File tnFileExist = new File(archiveFolder + transactionFile);
		if (tnFileExist.exists()) {
			currentMap = loadJson(archiveFolder + transactionFile);
	        System.out.println("json size " + currentMap.size());
		}
		
        newMap = loadCsv();
        System.out.println("csv size " + newMap.size());


        newMap.putAll(currentMap);
        System.out.println("merged size " + newMap.size());
        
        writeJson(newMap, archiveFolder + transactionFile, "CURRENT");


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
            mapper.writeValue(new File(fileName), acc);
//            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
//            DateFormat df = new SimpleDateFormat("yyyyMMdd");
//            mapper.setDateFormat(df);
//            mapper.writeValue(new File(fileName + "Formatted.json"), acc);
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
					Files.copy(file, FileSystems.getDefault().getPath(applicationFolder + fileName(file.toFile())),
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
