package net.sf.odinms.provider;

import net.sf.odinms.provider.wz.WZFile;
import net.sf.odinms.provider.xmlwz.XMLWZFile;

import java.io.File;
import java.io.IOException;

public class MapleDataProviderFactory {
    private static final String wzPath = System.getProperty("net.sf.odinms.wzpath");

    private static MapleDataProvider getWZ(Object in, boolean provideImages) {
        if (in instanceof File) {
            File fileIn = (File) in;
            if (fileIn.getName().endsWith("wz") && !fileIn.isDirectory()) {
                try {
                    return new WZFile(fileIn, provideImages);
                } catch (IOException e) {
                    throw new RuntimeException("Loading WZ File failed", e);
                }
            } else {
                // always provides images as we do this lazily and it's
                // therefore cheap (assuming that the images don't get loaded
                // for fun)
                return new XMLWZFile(fileIn);
            }
        }
        throw new IllegalArgumentException("Can't create data provider for input " + in);
    }

    public static MapleDataProvider getDataProvider(Object in) {
        return getWZ(in, false);
    }

    public static MapleDataProvider getImageProvidingDataProvider(Object in) {
        return getWZ(in, true);
    }

    public static File fileInWZPath(String filename) {
        return new File(wzPath, filename);
    }
}
