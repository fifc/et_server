#!/bin/zsh

cd `dirname $0`
geth=geth/geth
#${geth} --networkid 19 --port 7018 --bootnodes "enode://fdbe569cb928e54de18f0f507ff975afa4b0cff2cbe844dc59ffb2f15909f9e5d2e6296f83afc51cf47087926d5fad4af0674ae026995b778bf09d0c7c37f0de@103.43.70.126:7017" --etherbase="0xf4fa296e2af4d4378c472ab8a4fbc33bbdcfdaef" #--rpcapi personal,db,eth,net,web3
#${geth} --datadir=/home/etherum/light --port 7018 --syncmode light --rpcapi personal,db,eth,net,web3
nohup ${geth} --datadir=/home/etherum/light --syncmode light --rpc --rpcaddr '[::]' --rpcport 8000 --rpcvhosts myserver.com > /dev/null &

