pwd=$PWD
workdir=$HOME/.cache/pom-cli

mkdir -p $workdir
cd $workdir

v=0.9.4
dist_name="pomcli-${v}-linux-x86_64"
archive_name="${dist_name}.tar.gz"

mkdir -p $HOME/.local/share/pom-cli

curl -v -O -L "https://github.com/andirady/pom-cli/releases/download/v${v}/${archive_name}"
if [ ! -f $archive_name ]; then
    echo "Download fail" >&2
    exit 1
fi

tar xzf $archive_name -C $HOME/.local/bin/ $dist_name/bin/pom --strip-components=2
tar xzf $archive_name -C $HOME/.local/share/pom-cli/ $dist_name/LICENSE.txt --strip-components=1

cd $pwd
rm -rf $workdir
