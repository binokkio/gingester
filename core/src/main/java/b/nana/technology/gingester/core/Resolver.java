package b.nana.technology.gingester.core;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Resolver {

    private final Set<String> packages;
    private final Map<String, String> capitalization;

    public Resolver(Set<String> packages) {
        this(packages, Map.of());
    }

    public Resolver(Set<String> packages, Map<String, String> capitalization) {
        this.packages = packages;
        this.capitalization = capitalization;

        capitalization.forEach((lowercase, capitalized) -> {
            if (!capitalized.toLowerCase(Locale.ENGLISH).equals(lowercase)) {
                throw new IllegalArgumentException("Bad capitalization: " + lowercase + " -> " + capitalized);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public final Optional<Class<Transformer<?, ?>>> resolve(String name) {

        int lastIndexOfPeriod = name.lastIndexOf('.');
        String finalName = lastIndexOfPeriod == -1 ?
                name :
                name.substring(0, lastIndexOfPeriod).toLowerCase(Locale.ENGLISH) + name.substring(lastIndexOfPeriod);

        Optional<Class<Transformer<?, ?>>> result = packages.stream()
                .map(pkg -> pkg + "." + finalName)
                .map(className -> {
                    try {
                        return (Class<Transformer<?, ?>>) Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        return null;
                    } catch (ClassCastException e) {
                        // TODO warn
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .reduce((a, b) -> { throw new IllegalStateException("Multiple matches for " + name); });

        if (result.isEmpty()) {
            return Optional.empty();
        } else {

            // validate name
            String[] parts = name.split("\\.");
            for (int i = 0; i < parts.length - 1; i++) {
                String lowerCased = parts[i].toLowerCase(Locale.ENGLISH);
                String capitalized = capitalization.get(lowerCased);
                if (capitalized == null) capitalized = upperCaseFirstChar(lowerCased);
                if (!parts[i].equals(capitalized)) return Optional.empty();  // TODO throw/warn?
            }

            return result;
        }
    }

    public final Optional<String> name(Transformer<?, ?> transformer) {
        String canonicalName = transformer.getClass().getCanonicalName();
        if (canonicalName == null) return Optional.empty();
        return name(canonicalName);
    }

    final Optional<String> name(String canonicalName) {
        return packages.stream()
                .filter(canonicalName::startsWith)
                .max(Comparator.comparingInt(pkg -> countOccurrences(".", pkg)))
                .map(pkg -> canonicalName.substring(pkg.length() + 1))
                .map(pkg -> pkg.split("\\."))
                .map(Arrays::stream)
                .map(parts -> parts.map(part -> {
                    String capitalized = capitalization.get(part);
                    if (capitalized != null) return capitalized;
                    return upperCaseFirstChar(part);
                }).collect(Collectors.joining(".")));
    }

    static String upperCaseFirstChar(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    static boolean onlyFirstCharIsCapitalized(String string) {
        return Character.isUpperCase(string.charAt(0)) &&
                string.substring(1).equals(string.substring(1).toLowerCase(Locale.ENGLISH));
    }

    static int countOccurrences(String needle, String haystack) {
        String without = haystack.replace(needle, "");
        int delta = haystack.length() - without.length();
        return delta / needle.length();
    }
}
