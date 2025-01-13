@REM 编译前要安装好go，gomobile和jdk11
gomobile bind -target=android -androidapi 21 -o ../app/libs/xyropencc.aar -ldflags "-s -w"