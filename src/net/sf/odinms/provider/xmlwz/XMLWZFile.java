package net.sf.odinms.provider.xmlwz;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataDirectoryEntry;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.wz.WZDirectoryEntry;
import net.sf.odinms.provider.wz.WZFileEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class XMLWZFile implements MapleDataProvider {
    private final File root;
    private final WZDirectoryEntry rootForNavigation;

    public XMLWZFile(File fileIn) {
        root = fileIn;
        rootForNavigation = new WZDirectoryEntry(fileIn.getName(), 0, 0, null);
        fillMapleDataEntitys(root, rootForNavigation);
    }

    private void fillMapleDataEntitys(File lroot, WZDirectoryEntry wzdir) {
        for (File file : lroot.listFiles()) {
            String fileName = file.getName();
            if (file.isDirectory() && !fileName.endsWith(".img")) {
                WZDirectoryEntry newDir = new WZDirectoryEntry(fileName, 0, 0, wzdir);
                wzdir.addDirectory(newDir);
                fillMapleDataEntitys(file, newDir);
            } else if (fileName.endsWith(".xml")) {
                wzdir.addFile(new WZFileEntry(fileName.substring(0, fileName.length() - 4), 0, 0, wzdir));
            }
        }
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    @Override
    public MapleData getData(String path) {
        File dataFile = new File(root, path + ".xml");
        File imageDataDir = new File(root, path);
        boolean exists = true;
        if (!dataFile.exists()) {
            //throw new RuntimeException("Datafile " + path + " does not exist in " + root.getAbsolutePath());
            exists = false;
        }
        FileInputStream fis;
        try {
            fis = new FileInputStream(dataFile);
        } catch (FileNotFoundException e) {
            //throw new RuntimeException("Datafile " + path + " does not exist in " + root.getAbsolutePath());
            fis = null;
            exists = false;
        }
        final XMLDomMapleData domMapleData;
        try {
            if (exists) {
                domMapleData = new XMLDomMapleData(fis, imageDataDir.getParentFile());
            } else {
                domMapleData = null;
                /*
                System.err.println(
                    "Datafile " +
                        path +
                        " does not exist in " +
                        root.getAbsolutePath() +
                        ". exists = false in XMLWZFile.getData()"
                );
                */
            }
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return domMapleData;
    }

    @Override
    public MapleDataDirectoryEntry getRoot() {
        return rootForNavigation;
    }
}
