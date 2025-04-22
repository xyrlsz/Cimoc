@REM 编译前要安装好go，gomobile和jdk11
cd opencc
sed -i 's/git diff dictionary\//git --no-pager diff dictionary\//' merge-data.sh
make update:data
cd ..
@REM 更新词典
gomobile bind -target=android -androidapi 21 -o ../app/libs/xyropencc.aar -ldflags "-s -w"