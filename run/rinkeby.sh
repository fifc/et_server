#!/bin/zsh

geth=/root/geth/geth
datadir=/home/etherum/rinkeby
etherbase=--etherbase=0xbecac26346d9711e39bddc87acc699997ddc7ff8
#${geth} --datadir=${datadir} --rinkeby --port 7018 --bootnodes "enode://fdbe569cb928e54de18f0f507ff975afa4b0cff2cbe844dc59ffb2f15909f9e5d2e6296f83afc51cf47087926d5fad4af0674ae026995b778bf09d0c7c37f0de@103.43.70.126:7017" ${etherbase} --rpcapi personal,db,eth,net,web3

#nohup ${geth} --datadir=${datadir} --rinkeby --syncmode fast ${etherbase} --rpc --rpcaddr 103.43.71.126 --rpcport 8000 --rpcvhosts eth.gboot.cc > /dev/null &
nohup ${geth} --datadir=${datadir} --rinkeby --syncmode fast ${etherbase} --rpc --rpcaddr '[::]' --rpcport 8000 --rpcvhosts e.gboot.cc > /dev/null &

