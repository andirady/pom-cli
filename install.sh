v=$(curl -si https://github.com/andirady/pom-cli/releases/latest | grep location | tr -d '\r\n' | sed -n 's#.*/tag/v\(.*\)$#\1#p')

pwd=$PWD
workdir=$HOME/.cache/pom-cli

if [ -z "$PREFIX" ]; then
    if [[ $EUID -eq 0 ]]; then
        PREFIX=/usr/local
    else
        PREFIX=$HOME/.local
    fi
fi

mkdir -p $workdir
cd $workdir

uname_m=`uname -m`
case "$uname_m" in
    x86_64|amd64) arch="x86_64" ;;
    aarch64|arm64) arch="aarch64" ;;
    *) arch="$uname_m" ;; # fallback to raw
esac

dist_name="pom-cli-${v}-linux-${arch}"
archive_name="${dist_name}.tar.gz"

mkdir -p $PREFIX/share/pom-cli

curl -sO -L "https://github.com/andirady/pom-cli/releases/download/v${v}/${archive_name}"
if [ ! -f $archive_name ]; then
    echo "Download fail" >&2
    exit 1
fi

mkdir -p $PREFIX/bin
tar xzf $archive_name -C $PREFIX/bin/ $dist_name/bin/pom --strip-components=2
tar xzf $archive_name -C $PREFIX/share/pom-cli/ $dist_name/LICENSE.txt --strip-components=1

cd $pwd
rm -rf $workdir
