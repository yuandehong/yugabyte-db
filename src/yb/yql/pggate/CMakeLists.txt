# Copyright (c) YugaByte, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
# in compliance with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License
# is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
# or implied.  See the License for the specific language governing permissions and limitations
# under the License.
#

set(PGGATE_SRCS
    ybc_pggate.cc
    pggate.cc
    pg_session.cc
    pg_statement.cc
    pg_ddl.cc
    pg_insert.cc)

set(PGGATE_LIBS
    yb_util
    yb_common
    yb_client)

ADD_YB_LIBRARY(yb_pggate
               SRCS ${PGGATE_SRCS}
               DEPS ${PGGATE_LIBS})
