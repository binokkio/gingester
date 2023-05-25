#!/bin/sh

gin() {
	directory="$HOME/.m2/repository/b/nana/technology/gingester/executable"
	jar=$(find ${directory} -iname '*.jar' -printf "%T@ %p\n" | sort -nr | cut -d ' ' -f2- | head -n 1)
	java -jar ${jar} "$@"
}

ginpd() {
	gin -t PathDef "$@"
}

ginps() {
	gin -t PathSearch "$@"
}

gind() {
	curl -d "$*" http://localhost:8765
}
