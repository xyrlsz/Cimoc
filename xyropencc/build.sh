# 编译前要安装好go，gomobile和jdk11
# 更新词典
cd opencc
sed -i 's/git diff dictionary\//git --no-pager diff dictionary\//' merge-data.sh
make update:data
cd ..
gomobile bind -target=android -androidapi 21 -o ../app/libs/xyropencc.aar -ldflags "-s -w"