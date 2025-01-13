package xyropencc

import (
	"github.com/longbridgeapp/opencc"
)

func S2T(str string) (string, error) {
	s2t, err := opencc.New("s2t")
	if err != nil {
		return "", err
	}
	out, err := s2t.Convert(str)
	if err != nil {
		return "", err
	}
	return out, nil
}
