package b.nana.technology.gingester.transformers.git.giterate;

import java.time.ZonedDateTime;

class Commit {

    final String hash;
    final ZonedDateTime time;

    Commit(String hash, ZonedDateTime time) {
        this.hash = hash;
        this.time = time;
    }
}
