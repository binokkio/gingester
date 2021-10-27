if [ ! -f "pom.xml" ]; then
	echo "This script must be run from the root of the Gingester repository"
	exit 1
fi

artifactId=`pcregrep -e '^    <artifactId>(.*)</artifactId>$' -o1 pom.xml`
version=`pcregrep -e '^    <version>(.*)</version>$' -o1 pom.xml`

if [ $artifactId != gingester ]; then
	echo "This script must be run from the root of the Gingester repository"
	exit 2
fi

mvn clean package
docker build -f docker/local/Dockerfile . -t $artifactId:$version
