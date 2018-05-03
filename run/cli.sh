#!/usr/bin/zsh

exec `dirname $0`/mnl/bin/omnicore-cli -datadir=/home/bitdat $*
