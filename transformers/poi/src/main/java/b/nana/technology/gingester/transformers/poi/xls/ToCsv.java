package b.nana.technology.gingester.transformers.poi.xls;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.transformer.Transformer;
import b.nana.technology.gingester.transformers.base.common.iostream.OutputStreamWrapper;
import b.nana.technology.gingester.transformers.poi.SheetSplittingOutputStream;
import org.apache.poi.examples.hssf.eventusermodel.XLS2CSVmra;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class ToCsv implements Transformer<InputStream, OutputStreamWrapper> {

    @Override
    public void transform(Context context, InputStream in, Receiver<OutputStreamWrapper> out) throws IOException {

        try (PrintStream printStream = new PrintStream(new SheetSplittingOutputStream(true,
                (sheetName, sheet) -> out.accept(context.stash("description", sheetName), sheet)))) {
            new XLS2CSVmra(new POIFSFileSystem(in), printStream, -1).process();
        }
    }
}
