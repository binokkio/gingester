package b.nana.technology.gingester.transformers.protobuf;

import com.google.protobuf.util.JsonFormat;

public abstract class ToJsonBase {

    private final JsonFormat.Printer printer;

    public ToJsonBase(Parameters parameters) {
        JsonFormat.Printer printer = JsonFormat.printer();
        if (parameters.includingDefaultValueFields) printer = printer.includingDefaultValueFields();
        if (parameters.omittingInsignificantWhitespace) printer = printer.omittingInsignificantWhitespace();
        if (parameters.printingEnumsAsInts) printer = printer.printingEnumsAsInts();
        if (parameters.preservingProtoFieldNames) printer = printer.preservingProtoFieldNames();
        if (parameters.sortingMapKeys) printer = printer.sortingMapKeys();
        this.printer = printer;
    }

    public JsonFormat.Printer getPrinter() {
        return printer;
    }

    public static class Parameters {
        public boolean includingDefaultValueFields;
        public boolean omittingInsignificantWhitespace;
        public boolean printingEnumsAsInts;
        public boolean preservingProtoFieldNames;
        public boolean sortingMapKeys;
    }
}
