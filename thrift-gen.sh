#!/bin/sh

thrift --gen java -o src/main/thrift src/main/thrift/runner.thrift
thrift --gen csharp -o src/main/csharp src/main/thrift/runner.thrift
