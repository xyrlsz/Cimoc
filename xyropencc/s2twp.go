package xyropencc

import (
	"github.com/longbridge/opencc"
	"sync"
)

var (
	s2twpInstance *opencc.OpenCC
	s2twpOnce     sync.Once
)

func S2TWP(str string) (string, error) {
	var err error
	s2twpOnce.Do(func() {
		s2twpInstance, err = opencc.New("s2twp")
	})
	if err != nil {
		return "", err
	}
	out, err := s2twpInstance.Convert(str)
	if err != nil {
		return "", err
	}
	return out, nil
}
