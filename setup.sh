#! /bin/bash

# set -euo pipefail

finch_dir=`dirname "${BASH_SOURCE[0]}"`

finch() {
    echo "Java finch install directory: $finch_dir"

    rm -f "$finch_dir/.env.sh"

    bb --classpath "$finch_dir/src" --main java-finch.core $@

    if [[ -f "$finch_dir/.env.sh" ]]; then
        echo "Setting JAVA_HOME from $finch_dir/.env.sh"
        source "$finch_dir/.env.sh"
    fi
}
