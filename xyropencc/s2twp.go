package xyropencc

import (
	"github.com/longbridgeapp/opencc"
)

func S2TWP(str string) (string, error) {
	tw2sp, err := opencc.New("tw2sp")
	if err != nil {
		return "", err
	}
	out, err := tw2sp.Convert(str)
	if err != nil {
		return "", err
	}
	return out, nil
}
