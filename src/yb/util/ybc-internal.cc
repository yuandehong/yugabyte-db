// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.

#include "yb/util/ybc-internal.h"

#include "yb/util/logging.h"

namespace yb {

namespace {
YBCPAllocFn g_palloc_fn = nullptr;
}

void YBCSetPAllocFn(YBCPAllocFn palloc_fn) {
  CHECK_NOTNULL(palloc_fn);
  g_palloc_fn = palloc_fn;
}

void* YBCPAlloc(size_t size) {
  CHECK_NOTNULL(g_palloc_fn);
  return g_palloc_fn(size);
}

YBCStatus ToYBCStatus(const Status& status) {
  if (status.ok()) {
    return nullptr;
  }
  std::string status_str = status.ToString();
  size_t status_msg_buf_size = status_str.size() + 1;
  YBCStatus ybc_status = reinterpret_cast<YBCStatus>(
      YBCPAlloc(sizeof(YBCStatusStruct) + status_msg_buf_size));
  ybc_status->code = status.code();
  strncpy(ybc_status->msg, status_str.c_str(), status_msg_buf_size);
  return ybc_status;
}

YBCStatus YBCStatusOK() {
  return nullptr;
}

YBCStatus YBCStatusNotSupport(const string& feature_name) {
  if (feature_name.empty()) {
    return ToYBCStatus(STATUS(NotSupported, "Feature is not supported"));
  } else {
    return ToYBCStatus(STATUS_FORMAT(NotSupported, "Feature '$0' not supported", feature_name));
  }
}

} // namespace yb
