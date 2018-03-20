#!/usr/bin/env bash

set -x

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -d|--base-dir)
    BASE_DIR="$2"
    shift # past argument
    shift # past value
    ;;
    -u|--pipewrench-git-repo)
    GIT_REPO="$2"
    shift # past argument
    shift # past value
    ;;
    -p|--pipewrench-dir)
    PIPEWRENCH_DIR="$2"
    shift # past argument
    shift # past value
    ;;
    -c|--pipewrench-ingest-dir)
    INGEST_DIR="$2"
    shift # past argument
    shift # past value
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

if [ ! -d "$BASE_DIR" ]; then
    echo "Base directory: $BASE_DIR does not exist creating..."
    mkdir -p $BASE_DIR
fi

if [ ! -d "$PIPEWRENCH_DIR" ]; then
    echo "Pipewrench repo does not exist in directory $PIPEWRENCH_DIR cloning from $GIT_REPO..."
    git clone $GIT_REPO
fi

if [ ! -d "$INGEST_DIR" ]; then
    echo "Ingest configuration directory: $INGEST_DIR does not exist creating..."
    mkdir -p $INGEST_DIR
fi

if [ ! -f "$BASE_DIR/generate-scripts.sh" ]; then
    echo "generate-scripts.sh not found in $BASE_DIR copying from pipeforge source..."
    cp generate-scripts.sh $BASE_DIR
fi



