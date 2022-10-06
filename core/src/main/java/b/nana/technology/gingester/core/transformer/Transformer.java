package b.nana.technology.gingester.core.transformer;

import b.nana.technology.gingester.core.annotations.Passthrough;
import b.nana.technology.gingester.core.annotations.Stashes;
import b.nana.technology.gingester.core.configuration.SetupControls;
import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.receiver.Receiver;
import net.jodah.typetools.TypeResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Transformer.
 * <p>
 * This is the root interface of all transformations in the Gingester framework.
 * <p>
 * The following code is an example of a basic transformer that takes a string as input and outputs that string
 * with an appended exclamation mark.
 * <pre>
 * public class Emphasize implements Transformer&lt;String, String&gt; {
 *
 *     &#64;Override
 *     public void transform(Context context, String in, Receiver&lt;String&gt; out) {
 *         out.accept(context, in + '!');
 *     }
 * }
 * </pre>
 * <p>
 * TODO document exception handling
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public interface Transformer<I, O> {

    @SuppressWarnings("unchecked")
    default Class<? extends I> getInputType() {
        return (Class<I>) TypeResolver.resolveRawArguments(Transformer.class, getClass())[0];
    }

    @SuppressWarnings("unchecked")
    default Class<? extends O> getOutputType() {
        return (Class<O>) TypeResolver.resolveRawArguments(Transformer.class, getClass())[1];
    }

    default boolean isPassthrough() {
        return getClass().getAnnotation(Passthrough.class) != null;
    }

    @SuppressWarnings("unchecked")
    default Map<String, Object> getStashDetails() {
        Map<String, Object> stashDetails = new HashMap<>();
        Stashes[] stashes = getClass().getAnnotationsByType(Stashes.class);
        for (Stashes stash : stashes) {
            Map<String, Object> pointer = stashDetails;
            String[] parts = stash.stash().split("\\.");
            for (int i = 0; i < parts.length - 1; i++)
                pointer = (Map<String, Object>) pointer.computeIfAbsent(parts[i], p -> new HashMap<>());
            pointer.put(parts[parts.length - 1], stash.type());
        }
        return stashDetails;
    }

    /**
     * Transformer setup.
     * <p>
     * This is the first method called on this transformer during a Gingester run. Transformers with specific
     * requirements can implement it and use the given {@link SetupControls} to get the setup they require.
     * <p>
     * If this transformer overrides {@link Transformer#prepare(Context, Receiver)} or
     * {@link Transformer#finish(Context, Receiver)} the given {@link SetupControls} will be pre-configured to
     * sync with __seed__.
     * <p>
     * The syncs set up here only take effect if no syncs were specified in the run configuration.
     * <p>
     * The default implementation does nothing.
     *
     * @param controls the controls available for setting up this transformer
     */
    default void setup(SetupControls controls) {}

    /**
     * Transformer open.
     * <p>
     * Called by a thread dedicated to this transformer. Everything done by this method "happens-before" the
     * first call to {@code prepare}, if any, and {@code transform}.
     * <p>
     * The default implementation does nothing.
     *
     * @throws Exception anything, see {@link Transformer} javadoc for exception handling details
     */
    default void open() throws Exception {}

    /**
     * Transformer prepare.
     * <p>
     * Called only when this transformer is "synced" with another transformer. Everything done by this method
     * "happens-before" the first call to {@code transform} for the given {@code context}. If this transformer
     * is in sync with a transformer called Foo, {@code prepare} will be called once for every output of Foo.
     * <p>
     * The default implementation does nothing.
     *
     * @param context the context from the synced transformer
     * @param out output receiver
     * @throws Exception anything, see {@link Transformer} javadoc for exception handling details
     */
    default void prepare(Context context, Receiver<O> out) throws Exception {}

    default void beforeBatch(Context context) throws Exception {}
    default void afterBatch(Context context) throws Exception {}

    /**
     * Transformer transform.
     *
     * @param context the context for the given input
     * @param in input
     * @param out output receiver
     * @throws Exception anything, see {@link Transformer} javadoc for exception handling details
     */
    void transform(Context context, I in, Receiver<O> out) throws Exception;

    /**
     * Transformer finish.
     * <p>
     * Called only when this transformer is "synced" with another transformer. Everything done by this method
     * "happens-after" the last call to {@code transform} for the given {@code context}. If this transformer is
     * in sync with a transformer called Foo, {@code finish} will be called once for every output of Foo.
     * <p>
     * The default implementation does nothing.
     *
     * @param context the context from the synced transformer
     * @param out output receiver
     * @throws Exception anything, see {@link Transformer} javadoc for exception handling details
     */
    default void finish(Context context, Receiver<O> out) throws Exception {}

    /**
     * Transformer close.
     * <p>
     * Called by a thread dedicated to this transformer. Everything done by this method "happens-after" the last
     * call to {@code finish}, if any, and {@code transform}.
     * <p>
     * The default implementation does nothing.
     *
     * @throws Exception anything, see {@link Transformer} javadoc for exception handling details
     */
    default void close() throws Exception {}

    default String onReport() {
        return "";
    }
}
