package b.nana.technology.gingester.transformers.poi.xlsx;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import b.nana.technology.gingester.transformers.poi.SheetSplittingOutputStream;
import org.apache.poi.examples.xssf.eventusermodel.XLSX2CSV;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class ToCsv implements Transformer<InputStream, OutputStreamWrapper> {

    @Override
    public void transform(Context context, InputStream in, Receiver<OutputStreamWrapper> out) throws IOException, OpenXML4JException, SAXException {
        try (PrintStream printStream = new PrintStream(new SheetSplittingOutputStream(false,
                (sheetName, sheet) -> out.accept(context.stash("description", sheetName), sheet)))) {
            new XLSX2CSV(OPCPackage.open(in), printStream, -1).process();
        }
    }
}
