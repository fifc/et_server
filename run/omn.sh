#!/usr/bin/zsh

cd `dirname $0` || err_exit "exit"
export PATH=`pwd -P`/mnl/bin:/usr/bin:/usr/sbin


if [[ "$1" = "-help" || "$1" = "--help" ]]; then
	echo "usage: $0 [status|start|stop]"
	exit 0
fi

err_exit() {
	echo $1
	exit
}

proc_name=omnicored

get_pid() {
	pids=`pidof ${proc_name}`
	for p in `echo $pids`; do
		tmp=`ps -oargs -p $p | egrep 'datadir'`
		if [ -n "$tmp" ]; then
			return $p
		fi
	done
	return -1
}

stop_proc() {
	while (true) do
		get_pid
		pid=$?
		if (( $pid < 0 )) then
			return 0
		fi

		echo "stopping $pid ..."
		kill $pid
		i=0
		for (( ; i < 50; ++i)); do
			get_pid
			if (( $? < 0 )) then
				break
			fi
			sleep 0.1
		done

		if (( $i >= $1 * 10 )) then
			break
		fi
	done

	return 1
}

server_status() {
	get_pid
	pid=$?
	if (( $pid < 0 )) then
		echo "server is not running ..."
	else
		stime=`ps -olstart -p $pid|grep -v STARTED`
		echo "server($pid) is running since [$stime] ..."
	fi
}

if [[ "$0" =~ '.*\.status$' || "$1" = "status" ]]; then
	server_status
	exit 0
fi

if [[ -z "$1" ]]; then
	get_pid
	pid=$?
	if (( $pid > 0 )) then
		server_status
		exit 0
	fi
fi

if ! ( stop_proc 10 ) then
	echo "error stop server!"
	exit 1
fi

if [[ "$0" =~ '.*\.stop$' || "$1" = 'stop' ]]; then
	exit 0
fi

omnicored -datadir=/home/bitdat -txindex -daemon

get_pid
pid=$?
if (( $pid < 0 )) then
	echo "failed"
else
	echo "started. pid: $pid"
fi

exit 0
