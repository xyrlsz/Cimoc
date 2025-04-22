package xyropencc

import (
	"github.com/longbridge/opencc"
	"sync"
)

var (
	tw2spInstance *opencc.OpenCC
	tw2spOnce     sync.Once
)

func TW2SP(str string) (string, error) {
	var err error
	tw2spOnce.Do(func() {
		tw2spInstance, err = opencc.New("tw2sp")
	})
	if err != nil {
		return "", err
	}
	out, err := tw2spInstance.Convert(str)
	if err != nil {
		return "", err
	}
	return out, nil
}
