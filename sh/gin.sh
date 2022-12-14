#!/bin/sh

gin() {
	directory="$HOME/.m2/repository/b/nana/technology/gingester/executable"
	version=$(ls -t ${directory} | grep -v .xml | head -n 1)
	java -jar ${directory}/${version}/executable-${version}.jar "$@"
}

ginpd() {
	gin -t PathDef "$@"
}

ginps() {
	gin -t PathSearch "$@"
}
