#!/bin/zsh

cd `dirname $0`
geth=geth/geth
datadir="--datadir=/home/etherum/fast"
coinbase='--etherbase=0x0aee4d2f677a05d5859d933fab26bcc4efb3acf1'
bootnodes='--bootnodes "enode://fdbe569cb928e54de18f0f507ff975afa4b0cff2cbe844dc59ffb2f15909f9e5d2e6296f83afc51cf47087926d5fad4af0674ae026995b778bf09d0c7c37f0de@103.43.70.126:7017"'

#${geth} --port 7018 ${datadir} ${bootnodes} ${etherbase} --rpcapi personal,db,eth,net,web3
nohup ${geth} --port 7018 ${datadir} --syncmode fast ${coinbase} > /dev/null &
#${geth} --port 7018 ${datadir} --syncmode fast ${coinbase}

