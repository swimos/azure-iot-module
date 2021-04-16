// Copyright 2015-2019 SWIM.AI inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

open module swim.iot {
  requires transitive swim.api;
  requires swim.server;
  requires swim.client;
  requires com.azure.messaging.eventhubs;
  requires org.apache.httpcomponents.httpasyncclient;
  requires org.apache.httpcomponents.httpclient;
  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpcore.nio;

  exports swim.iot;
  provides swim.api.plane.Plane with swim.iot.SwimPlane;
}