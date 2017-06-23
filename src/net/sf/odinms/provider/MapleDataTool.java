package net.sf.odinms.provider;

import net.sf.odinms.provider.wz.MapleDataType;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class MapleDataTool {
    private MapleDataTool() {
    }

    public static String getString(final MapleData data) {
        return (String) data.getData();
    }

    public static String getString(final MapleData data, final String def) {
        if (data == null || data.getData() == null) {
            return def;
        } else {
            return (String) data.getData();
        }
    }

    public static String getString(final String path, final MapleData data) {
        return getString(data.getChildByPath(path));
    }

    public static String getString(final String path, final MapleData data, final String def) {
        return getString(data.getChildByPath(path), def);
    }

    public static double getDouble(final MapleData data) {
        return (Double) data.getData();
    }

    public static float getFloat(final MapleData data) {
        return (Float) data.getData();
    }

    public static int getInt(final MapleData data) {
        if (data.getType() == MapleDataType.STRING) {
            return Integer.parseInt(getString(data));
        } else {
            return ((Integer) data.getData());
        }
    }

    public static int getInt(final MapleData data, final int def) {
        if (data == null || data.getData() == null) {
            return def;
        } else {
            if (data.getType() == MapleDataType.STRING) {
                return Integer.parseInt(getString(data));
            } else {
                return (Integer) data.getData();
            }
        }
    }

    public static int getInt(final String path, final MapleData data) {
        return getInt(data.getChildByPath(path));
    }

    public static int getIntConvert(final MapleData data) {
        if (data.getType() == MapleDataType.STRING) {
            return Integer.parseInt(getString(data));
        } else {
            return getInt(data);
        }
    }

    public static int getIntConvert(final String path, final MapleData data) {
        final MapleData d = data.getChildByPath(path);
        if (d == null) return -1;
        if (d.getType() == MapleDataType.STRING) {
            try {
                return Integer.parseInt(getString(d));
            } catch (final NumberFormatException nfe) {
                return -1;
            }
        } else {
            return getInt(d);
        }
    }

    public static int getInt(final String path, final MapleData data, final int def) {
        return getInt(data.getChildByPath(path), def);
    }

    public static int getIntConvert(final String path, final MapleData data, final int def) {
        final MapleData d = data.getChildByPath(path);
        if (d == null) return def;
        if (d.getType() == MapleDataType.STRING) {
            try {
                return Integer.parseInt(getString(d));
            } catch (final NumberFormatException nfe) {
                return def;
            }
        } else {
            return getInt(d, def);
        }
    }

    public static BufferedImage getImage(final MapleData data) {
        return ((MapleCanvas) data.getData()).getImage();
    }

    public static Point getPoint(final MapleData data) {
        return (Point) data.getData();
    }

    public static Point getPoint(final String path, final MapleData data) {
        return getPoint(data.getChildByPath(path));
    }

    public static Point getPoint(final String path, final MapleData data, final Point def) {
        final MapleData pointData = data.getChildByPath(path);
        if (pointData == null) return def;
        return getPoint(pointData);
    }

    public static String getFullDataPath(final MapleData data) {
        String path = "";
        MapleDataEntity myData = data;
        while (myData != null) {
            path = myData.getName() + "/" + path;
            myData = myData.getParent();
        }
        return path.substring(0, path.length() - 1);
    }
}
