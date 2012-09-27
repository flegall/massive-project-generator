package com.lobstre.massive;

import java.io.File;
import java.util.Locale;

public class ProjectGenerator {

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);

		if (args.length == 2) {
			String dir = args[0];
			final File dirFile = new File(dir);
			String projectCountStr = args[1];
			int projectCount = Integer.parseInt(projectCountStr);
			check (dirFile, projectCount);
			generate (dirFile, projectCount);
		} else {
			System.out.println("Usage : ProjectGenerator directory projectCount");
		}

	}

	private static void check(File dirFile, int projectCount) {
		if (dirFile.exists()) {
			if (!dirFile.isDirectory()) {
				System.out.println(dirFile + " is not a directory");
				System.exit(-1);
			} else if (dirFile.list().length != 0) {
				System.out.println(dirFile + " is not empty");
				System.exit(-1);
			}
		} else {
			if (!dirFile.mkdirs()) {
				System.out.println(dirFile + " counldn't be created");
				System.exit(-1);
			}
		}
		
		if (projectCount < 0) {
			System.out.println("project count must be > 0");
			System.exit(-1);
		}
	}

	private static void generate(File dirFile, int projectCount) {
		
	}
}
