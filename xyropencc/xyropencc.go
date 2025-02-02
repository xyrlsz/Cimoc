package xyropencc

import (
	"github.com/longbridgeapp/opencc"
	"sync"
)

var (
	s2tInstance   *opencc.OpenCC
	s2tOnce       sync.Once
	tw2spInstance *opencc.OpenCC
	tw2spOnce     sync.Once
	t2sInstance   *opencc.OpenCC
	t2sOnce       sync.Once
	s2twpInstance *opencc.OpenCC
	s2twpOnce     sync.Once
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
