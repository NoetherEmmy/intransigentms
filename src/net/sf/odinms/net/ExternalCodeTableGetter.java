package net.sf.odinms.net;

import net.sf.odinms.tools.HexTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ExternalCodeTableGetter {
    final Properties props;

    public ExternalCodeTableGetter(Properties properties) {
        props = properties;
    }

    private static <T extends Enum<? extends IntValueHolder> & IntValueHolder> T valueOf(String name, T[] values) {
        for (T val : values) {
            if (val.name().equals(name)) return val;
        }
        return null;
    }

    private <T extends Enum<? extends IntValueHolder> & IntValueHolder> int getValue(String name,
                                                                                     T[] values,
                                                                                     int def) {
        String prop = props.getProperty(name);
        if (prop != null && !prop.isEmpty()) {
            String trimmed = prop.trim();
            String[] args = trimmed.split(" ");
            int base = 0;
            String offset;
            if (args.length == 2) {
                base = valueOf(args[0], values).getValue();
                if (base == def) {
                    base = getValue(args[0], values, def);
                }
                offset = args[1];
            } else {
                offset = args[0];
            }
            if (offset.length() > 2 && offset.substring(0, 2).equals("0x")) {
                return Integer.parseInt(offset.substring(2), 16) + base;
            } else {
                return Integer.parseInt(offset) + base;
            }
        }
        return def;
    }

    public static <T extends Enum<? extends WritableIntValueHolder> & WritableIntValueHolder>
                  String getOpcodeTable(T[] enumeration) {
        StringBuilder enumVals = new StringBuilder();
        List<T> all = new ArrayList<>(Arrays.asList(enumeration));
        all.sort((o1, o2) -> Integer.valueOf(o1.getValue()).compareTo(o2.getValue()));
        for (T code : all) {
            enumVals.append(code.name());
            enumVals.append(" = ");
            enumVals.append("0x");
            enumVals.append(HexTool.toString(code.getValue()));
            enumVals.append(" (");
            enumVals.append(code.getValue());
            enumVals.append(")\n");
        }
        return enumVals.toString();
    }

    public static <T extends Enum<? extends WritableIntValueHolder> & WritableIntValueHolder>
                  void populateValues(Properties properties, T[] values) {
        ExternalCodeTableGetter exc = new ExternalCodeTableGetter(properties);
        for (T code : values) {
            code.setValue(exc.getValue(code.name(), values, -2));
        }
        Logger log = LoggerFactory.getLogger(ExternalCodeTableGetter.class);
        if (log.isTraceEnabled()) {
            log.trace(getOpcodeTable(values));
        }
    }
}
