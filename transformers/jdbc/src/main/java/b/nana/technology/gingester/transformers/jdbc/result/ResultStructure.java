package b.nana.technology.gingester.transformers.jdbc.result;

import java.sql.ResultSet;
import java.util.Map;

public interface ResultStructure {
    Map<String, Object> readRow(ResultSet resultSet);
}
