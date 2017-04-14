package com.immelja;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import au.com.bytecode.opencsv.CSVReader;

public class App {
	public static void main(String[] args) throws IOException, ParseException {
		System.out.println("spentrack");
		scanDownloads();
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
		String applicationFolder = "./in/";
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
