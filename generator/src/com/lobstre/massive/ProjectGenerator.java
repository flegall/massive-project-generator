package com.lobstre.massive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectGenerator {

	public static void main(String[] args) throws IOException {
		Locale.setDefault(Locale.US);

		if (args.length == 2) {
			String dir = args[0];
			final File dirFile = new File(dir);
			String projectCountStr = args[1];
			int projectCount = Integer.parseInt(projectCountStr);
			check(dirFile, projectCount);
			generateWorkspace(dirFile, projectCount);
		} else {
			System.out
					.println("Usage : ProjectGenerator directory projectCount");
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

	private static void generateWorkspace(final File workspacedir, final int projectCount) throws IOException {
		// CPUs
        final int cpus = Runtime.getRuntime ().availableProcessors ();
        final ExecutorService es = Executors.newFixedThreadPool (cpus);
        final AtomicInteger counter = new AtomicInteger(0);
		for (int i = 0; i < projectCount; i++) {
			final int iFinal = i;
			es.submit(new Runnable () {
				@Override
				public void run() {
					try {
						generateProject(iFinal, workspacedir);
						System.out.println("Done #" + iFinal +" : " + counter.incrementAndGet()+  " / " + projectCount);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			});
		}
		// Shutdown worker.
		es.shutdown ();
		try {
			es.awaitTermination (3600L, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void generateProject(int projectNumber, File rootDir) throws IOException {
		final DecimalFormat df = new DecimalFormat("0000");
		final String projectName = "project_" + df.format(projectNumber);
		final File projectDir = makeSubDir(rootDir, projectName);
		final File srcDir = makeSubDir(projectDir, "src");
		final File comDir = makeSubDir(srcDir, "com");
		final File companyDir = makeSubDir(comDir, "company");
		final File productDir = makeSubDir(companyDir, "product");
		final File projectPackageDir = makeSubDir(productDir, projectName);
		for (int i = 0; i < 10; i++) {
			final String packageName = "package" + i;
			final File packageDir = makeSubDir(projectPackageDir, packageName);
			for (int j = 0; j < 10; j++) {
				final String subPackageName = "subpackage" + j;
				final File subPackageDir = makeSubDir(packageDir, subPackageName);
				for (int k = 0; k < 10; k++) {
					InputStream resource = null;
					FileOutputStream fos = null;
					File javaFile = null;
					final String className = "UberClass_" + projectName + df.format(i) + df.format(j) + df.format(k);
					try {
						final ClassLoader cl = ProjectGenerator.class.getClassLoader();
						resource = cl.getResourceAsStream("com/lobstre/massive/ClassTemplate.txt");
						final String fileName = className + ".java";
						javaFile = new File (subPackageDir, fileName);
						fos = new FileOutputStream (javaFile);
						copy (resource, fos);
					} finally {
						handleFinally(resource, fos);
						
					}
					rewrite (javaFile, ProjectGenerator.<String, String>asMap(new Object[][] {
							{"CLASS_NAME", className},
							{"PACKAGE_NAME", "com.company.product."+packageName + "." +subPackageName},
					}));
				}
			}
		}
		
		final ClassLoader cl = ProjectGenerator.class.getClassLoader();
		InputStream resource = null;
		FileOutputStream fos = null;
		File javaFile = new File (projectDir, ".project");
		
		try {
			resource = cl.getResourceAsStream("com/lobstre/massive/project.txt");
			fos = new FileOutputStream(javaFile);
			copy(resource, fos);
		} finally {
			handleFinally(resource, fos);
		}
		
	}

	private static void handleFinally(InputStream resource,
			FileOutputStream fos) throws IOException {
		if (resource != null) {
			try {
				resource.close();
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
		}
	}

	private static File makeSubDir(File dir, String subDirName) {
		final File projectDir = new File(dir, subDirName);
		projectDir.mkdir();
		return projectDir;
	}
	
    private static final int BUFFER_SIZE = 4096;
	private static final String UTF8_CHARSET = "UTF-8";
    private static ThreadLocal<byte[]> buffers = new ThreadLocal<byte[]>() {

        @Override
        protected byte[] initialValue() {
            return new byte[BUFFER_SIZE];
        }

    };
	
    private static void copy(InputStream inputStream, OutputStream outputStream)
            throws NullPointerException, IOException {
        byte[] buffer = buffers.get();
        int readBytes;
        while ((readBytes = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, readBytes);
        }
    }
    
    private static void rewrite (final File valuesFile, final Map<String, String> replacements) throws IOException {
        String file = readWholeFile (valuesFile);
        for (final Map.Entry<String, String> e : replacements.entrySet ()) {
            final Pattern pattern = Pattern.compile (e.getKey (), Pattern.LITERAL);
            final String replacementQuoted = Matcher.quoteReplacement (e.getValue ());
            final Matcher matcher = pattern.matcher (file);
            file = matcher.replaceAll (replacementQuoted);
        }
        writeFile (valuesFile, file);
    }

    private static String readWholeFile (final File file) throws IOException {
        final BufferedReader reader = new BufferedReader (new InputStreamReader (new FileInputStream (file), UTF8_CHARSET));
        String line = null;
        final StringBuilder stringBuilder = new StringBuilder ();
        final String ls = System.getProperty ("line.separator");
        while ((line = reader.readLine ()) != null) {
            stringBuilder.append (line);
            stringBuilder.append (ls);
        }
        reader.close ();
        return stringBuilder.toString ();
    }

    private static void writeFile (final File valuesFile, final String file) throws FileNotFoundException, UnsupportedEncodingException {
        final PrintWriter pw = new PrintWriter (valuesFile, UTF8_CHARSET);
        pw.write (file);
        pw.close ();
    }    
    
	@SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> asMap (final Object [][] in) {
	    final Map<K, V> ret = new LinkedHashMap <K, V> (in.length);
	    
	    for (Object[] keyValue : in) {
	        ret.put ((K)keyValue[0], (V)keyValue[1]);
	    }

	    return Collections.unmodifiableMap (ret);
	}    
    
}
