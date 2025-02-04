package xyropencc

import (
	"github.com/longbridgeapp/opencc"
	"sync"
)

var (
	s2tInstance *opencc.OpenCC
	s2tOnce     sync.Once
)

func S2T(str string) (string, error) {
	var err error
	s2tOnce.Do(func() {
		s2tInstance, err = opencc.New("s2t")
	})
	if err != nil {
		return "", err
	}
	out, err := s2tInstance.Convert(str)
	if err != nil {
		return "", err
	}
	return out, nil
}
