package net.sf.odinms.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassFinder {
    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClassFinder.class);

    final List<JarFile> jars = new ArrayList<>();
    final List<File> dirs = new ArrayList<>();

    public ClassFinder() {
        final String classpath = System.getProperty("java.class.path");
        final String[] splittedPath = classpath.split(File.pathSeparator);
        for (final String cpe : splittedPath) {
            final File cpeFile = new File(cpe);
            if (cpeFile.isDirectory()) {
                dirs.add(cpeFile);
            } else {
                try {
                    jars.add(new JarFile(cpeFile));
                } catch (final IOException e) {
                    System.err.println("ERROR");
                    e.printStackTrace();
                }
            }
        }
    }

    private void addClassesInFolder(final List<String> classes, final File folder, final String packageName, final boolean recurse) {
        for (final File f : folder.listFiles()) {
            if (!f.isDirectory()) {
                if (f.getName().endsWith(".class")) {
                    classes.add(packageName + "." + f.getName().substring(0, f.getName().length() - 6));
                }
            } else if (f.isDirectory() && recurse) {
                addClassesInFolder(classes, f, packageName + "." + f.getName(), recurse);
            }
        }
    }

    public String[] listClasses(final String packageName, final boolean recurse) {
        final List<String> ret = new ArrayList<>();

        // scan dirs
        final String fileSystemPackagePath = packageName.replace('.', File.separatorChar);
        for (final File dir : dirs) {
            final File subfolder = new File(dir, fileSystemPackagePath);
            if (subfolder.exists() && subfolder.isDirectory()) {
                addClassesInFolder(ret, subfolder, packageName, recurse);
            }
        }
        // scan jars
        final String jarPackagePath = packageName.replace('.', '/');
        for (final JarFile jar : jars) {
        for (final Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
            // Get the entry name
            final String entryName = (entries.nextElement()).getName();
            if (entryName.endsWith(".class") && entryName.startsWith(jarPackagePath)) {
                final int lastSlash = entryName.lastIndexOf('/');
                if (lastSlash <= jarPackagePath.length() || recurse) {
                    final String path = entryName.substring(0, lastSlash);
                    final String className = entryName.substring(lastSlash + 1, entryName.length() - 6);
                    ret.add(path.replace('/', '.') + "." + className);
                }
            }
        }
        }
        return ret.toArray(new String[ret.size()]);
    }

    public void dispose() {
        for (final JarFile jar : jars) {
            try {
                jar.close();
            } catch (final IOException e) {
                System.err.println("THROW");
                e.printStackTrace();
            }
        }
    }
}
