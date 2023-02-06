package b.nana.technology.gingester.transformers.base.transformers.path;

import b.nana.technology.gingester.core.controller.Context;
import b.nana.technology.gingester.core.template.TemplateParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefTest {

    @Test
    void testRootPathSegmentThrows() {

        Def.Parameters parameters = new Def.Parameters();
        parameters.trusted = new TemplateParameters("/foo/bar");
        parameters.untrusted = new TemplateParameters("/baz");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Def(parameters).transform(Context.newTestContext(), null, null));

        assertEquals("/baz wanders too far", e.getMessage());
    }

    @Test
    void testTooManyUpReferencesInSegmentThrows() {

        Def.Parameters parameters = new Def.Parameters();
        parameters.trusted = new TemplateParameters("/foo/bar");
        parameters.untrusted = new TemplateParameters("baz/../../qux");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Def(parameters).transform(Context.newTestContext(), null, null));

        assertEquals("baz/../../qux wanders too far", e.getMessage());
    }
}