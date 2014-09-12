package me.nallar.gradleprepatcher;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.jar.*;

public class Main {
	public static boolean extractGeneratedSource = false;
	public static File generatedSourceDirectory = new File("./generated/src/");
	public static File rootSourceDirectory;

	private static boolean patchesLoaded = false;

	/**
	 * Called to load patches from java source files in the given directory
	 *
	 * @param rootSourceDirectory Source root path
	 * @param patchDirectory      patch directory path (package containing Patch classes)
	 */
	public static void loadPatches(File rootSourceDirectory, File patchDirectory) {
		if (!patchDirectory.isDirectory()) {
			throw new IllegalArgumentException("patchDirectory must be a directory.");
		}
		if (!rootSourceDirectory.isDirectory()) {
			throw new IllegalArgumentException("rootSourceDirectory must be a directory.");
		}
		if (!patchDirectory.toString().startsWith(rootSourceDirectory.toString())) {
			throw new IllegalArgumentException("patchDirectory (" + patchDirectory + ") must be below rootSourceDirectory (" + rootSourceDirectory + ')');
		}
		Main.rootSourceDirectory = rootSourceDirectory;
		try {
			PrePatcher.loadPatches(patchDirectory);
			patchesLoaded = true;
		} catch (Throwable t) {
			t.printStackTrace();
			Throwables.propagate(t);
		}
	}

	/**
	 * @param path   File
	 * @param source if TRUE source, if FALSE binary
	 */
	public static void onTaskEnd(File path, boolean source) {
		if (!patchesLoaded) {
			throw new RuntimeException("me.nallar.gradleprepatcher.Main.loadPatches must be called first");
		}
		try {
			if (source) {
				prepatchSource(path);
			} else {
				prepatchBinary(path);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			Throwables.propagate(t);
		}
	}

	private static void prepatchBinary(File jar) throws Exception {
		// READING
		Map<String, byte[]> classBytesMap = new HashMap<String, byte[]>();
		JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
		JarEntry entry;
		while ((entry = jarInputStream.getNextJarEntry()) != null) {
			byte[] classBytes = ByteStreams.toByteArray(jarInputStream);
			if (entry.getName().endsWith(".class")) {
				// PARSING
				String name = entry.getName().replace('\\', '/');
				classBytes = PrePatcher.patchCode(classBytes, name);
			}
			classBytesMap.put(entry.getName(), classBytes);

			jarInputStream.closeEntry();
		}
		jarInputStream.close();

		// WRITING
		JarOutputStream ostream = new JarOutputStream(new FileOutputStream(jar));
		for (Map.Entry<String, byte[]> e : classBytesMap.entrySet()) {
			ostream.putNextEntry(new JarEntry(e.getKey()));
			ostream.write(e.getValue());
			ostream.closeEntry();
		}
		ostream.close();
	}

	private static void prepatchSource(File directory) throws Exception {
		File generatedSrcDirectory = null;

		if (extractGeneratedSource) {
			generatedSrcDirectory = generatedSourceDirectory.getCanonicalFile();
			if (generatedSrcDirectory.exists()) {
				deleteDirectory(generatedSrcDirectory.toPath());
			}
			generatedSrcDirectory.mkdirs();
		}

		final File mainSrcDirectory = new File("./src/main/java/");
		directory = directory.getCanonicalFile();
		final int cutoff = directory.toString().length();

		final File finalGeneratedSrcDirectory = generatedSrcDirectory;
		java.nio.file.Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
					throws IOException {
				String name = path.getFileName().toString();
				if (!name.endsWith(".java")) {
					return FileVisitResult.CONTINUE;
				}
				String fullPath = path.toFile().getCanonicalFile().toString();
				String partialPath = fullPath.substring(cutoff);
				if (new File(mainSrcDirectory, partialPath).exists()) {
					return FileVisitResult.CONTINUE;
				}
				String source = com.google.common.io.Files.toString(path.toFile(), Charsets.UTF_8);
				String patchedSource = PrePatcher.patchSource(source, partialPath);

				if (!patchedSource.equals(source)) {
					Files.write(path, patchedSource.getBytes(Charsets.UTF_8));
				}
				if (extractGeneratedSource) {
					File dest = new File(finalGeneratedSrcDirectory, partialPath);
					dest.getParentFile().mkdirs();
					Files.write(dest.toPath(), patchedSource.getBytes(Charsets.UTF_8));
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed; propagate exception
					throw exc;
				}
			}
		});
	}

	public static void deleteDirectory(Path path) throws IOException {
		java.nio.file.Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException {
				java.nio.file.Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				// try to delete the file anyway, even if its attributes
				// could not be read, since delete-only access is
				// theoretically possible
				java.nio.file.Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					java.nio.file.Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed; propagate exception
					throw exc;
				}
			}
		});
	}
}
