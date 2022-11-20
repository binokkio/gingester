package b.nana.technology.gingester.transformers.git.giterate;

import b.nana.technology.gingester.core.annotations.Names;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import b.nana.technology.gingester.core.template.Template;
import b.nana.technology.gingester.core.template.TemplateMapper;
import b.nana.technology.gingester.core.template.TemplateParameters;
import b.nana.technology.gingester.core.transformer.StashDetails;
import b.nana.technology.gingester.core.transformer.Transformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Names(1)
public final class Giterate implements Transformer<Object, Path> {

    private final Template originTemplate;
    private final Template branchTemplate;
    private final TemplateMapper<Path> scratchTemplate;
    private final Supplier<Giterator> giteratorSupplier;

    public Giterate(Parameters parameters) {
        originTemplate = Context.newTemplate(parameters.origin);
        branchTemplate = parameters.branch == null ? null :
                Context.newTemplate(parameters.branch);
        scratchTemplate = Context.newTemplateMapper(parameters.scratch, Paths::get);
        giteratorSupplier = () -> new TimeIntervalGiterator(parameters.interval);
    }

    @Override
    public StashDetails getStashDetails() {
        return giteratorSupplier.get().getStashDetails();
    }

    @Override
    public void transform(Context context, Object in, Receiver<Path> out) throws IOException, InterruptedException {

        Runtime runtime = Runtime.getRuntime();
        Path scratch = Files.createTempDirectory(scratchTemplate.render(context, in), "giterate-");

        // clone
        Path clone = scratch.resolve("clone");
        Process cloneProcess = runtime.exec(new String[] { "git", "clone", originTemplate.render(context, in), clone.toString() });
        int cloneResult = cloneProcess.waitFor();
        if (cloneResult != 0) throw new IllegalStateException("git clone did not exit with 0");

        // branch
        if (branchTemplate != null) {

            Process branchProcess = runtime.exec(new String[] { "git", "branch" }, null, clone.toFile());
            String currentBranch = new String(branchProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int branchResult = branchProcess.waitFor();
            if (branchResult != 0) throw new IllegalStateException("git branch did not exit with 0 but " + branchResult);

            String targetBranch = branchTemplate.render(context, in);

            if (!currentBranch.equals(targetBranch)) {
                Process checkoutProcess = runtime.exec(new String[] { "git", "checkout", targetBranch }, null, clone.toFile());
                int checkoutResult = checkoutProcess.waitFor();
                if (checkoutResult != 0) throw new IllegalStateException("git checkout did not exit with 0 but " + checkoutResult);
            }
        }

        // get commit hashes and dates
        List<Commit> commits = new ArrayList<>();
        Process logProcess = runtime.exec(new String[] { "git", "log", "--first-parent", "--format=%H %aI" }, null, clone.toFile());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] parts = line.split(" ");
            commits.add(new Commit(
                    parts[0],
                    ZonedDateTime.parse(parts[1])
            ));
        }
        int logResult = logProcess.waitFor();
        if (logResult != 0) throw new IllegalStateException("git log did not exit with 0");
        Collections.reverse(commits);

        // giterate
        giteratorSupplier.get().giterate(clone, commits, context, out);

        // remove scratch directory
        Files.walkFileTree(scratch, new SimpleFileVisitor<>(){

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static class Parameters {
        public TemplateParameters origin;
        public TemplateParameters branch;
        public TemplateParameters scratch = new TemplateParameters("/tmp", true);
        public String interval = "P1D";
    }
}
