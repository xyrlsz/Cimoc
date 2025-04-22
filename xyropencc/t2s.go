package xyropencc

import (
	"github.com/longbridge/opencc"
	"sync"
)

var (
	t2sInstance *opencc.OpenCC
	t2sOnce     sync.Once
)

func T2S(str string) (string, error) {
	var err error
	t2sOnce.Do(func() {
		t2sInstance, err = opencc.New("t2s")
	})
	if err != nil {
		return "", err
	}
	out, err := t2sInstance.Convert(str)
	if err != nil {
		return "", err
	}
	return out, nil
}
