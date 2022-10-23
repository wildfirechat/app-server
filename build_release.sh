if [ $# -eq 0 ]; then
    echo "Usage: sh build_release.sh version"
    exit -1
fi
echo "build release $1"

mvn clean package
cd target
cp -af ../config ./
cp -af ../systemd ./
cp -af ../release_note.md ./
tar -czvf app-server-release-$1.tar.gz app-$1.jar config systemd release_note.md
cp app-server-release-$1.tar.gz app-server-release-latest.tar.gz
cp app-server-0.61.rpm app-server-latest.rpm
cp app-server-0.61.deb app-server-latest.deb
