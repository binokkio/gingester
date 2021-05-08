# Gingester executable package

Just a pom.xml configured to build an executable jar containing the Gingester core and all transformers in the Gingester project.
Example usage:

```
java -jar executable.jar -t Std.In -t InputStream.ToString '{"delimiter":"\n"}' -t Std.Out '{"decorate":true}'
```