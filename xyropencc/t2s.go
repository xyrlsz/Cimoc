package xyropencc

import (
	"github.com/longbridgeapp/opencc"
)

func T2S(str string) (string, error) {
	t2s, err := opencc.New("t2s")
	if err != nil {
		return "", err
	}
	out, err := t2s.Convert(str)
	if err != nil {
		return "", err
	}
	return out, nil
}
