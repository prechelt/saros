#!/bin/bash 

# Used by called and sourced scripts
export CONFIG_DIR=travis/config \
       SCRIPT_DIR=travis/script/stf \
       ws_dir=$PWD

user="$(id -u):$(id -g)"
source "$SCRIPT_DIR/shared_vars.sh" # import: stf_master_name, CONTAINER_SRC, STF_CONTAINER_WS,...
source "$SCRIPT_DIR/config_utils.sh" # import: get_distinct_hosts

function restore_workspaces_permissions {
    local user="$1"

    docker exec -t "$stf_master_name" bash -c "chown -R $user $CONTAINER_SRC $STF_CONTAINER_WS"
}

function stop_and_remove_container {
    local container="$1"

    echo "::Stop and remove container: $container"
    docker stop "$container"
    docker rm "$container"
}

function stop_and_remove_slave_containers {
  get_distinct_hosts
  stf_slave_names="$retval"
  for slave_name in $stf_slave_names; do
    stop_and_remove_container "$slave_name"
  done
}

function remove_network {
    docker network rm "$stf_network_name"
}

function teardown_containers {
    stop_and_remove_container "$stf_master_name"
    stop_and_remove_slave_containers
    stop_and_remove_container "$stf_xmpp_server_name"
    remove_network
}

function finalize {
    local user="$1"; shift

    restore_workspaces_permissions "$user"
    teardown_containers
}

echo "::Pull images, start and configure containers"
$SCRIPT_DIR_HOST/stf/setup_stf_container.sh $PWD
if [ "$?" != "0" ]; then
    echo "::Failed to setup the containers"
    finalize
    exit 1
fi

echo "::Start stf tests"
docker exec -t "$stf_master_name" "$SCRIPT_DIR_CONTAINER/stf/master/start_stf_tests.sh" 
rc="$?"
if [ "$rc" != "0" ]; then
    echo "::Failed during the stf test execution"
fi
finalize
exit "$rc"
