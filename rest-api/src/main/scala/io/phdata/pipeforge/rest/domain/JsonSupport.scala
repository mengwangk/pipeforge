/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.phdata.pipeforge.rest.domain

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import io.phdata.pipeforge.jdbc.config.{ DatabaseConf, DatabaseType, ObjectType }
import io.phdata.pipewrench.domain.{ Column, PipewrenchConfig, Table }
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val environmentFormat = jsonFormat8(Environment)

  implicit def columnFormat           = jsonFormat5(Column)
  implicit def tableFormat            = jsonFormat7(Table)
  implicit def pipewrenchConfigFormat = jsonFormat1(PipewrenchConfig)

  def getDatabaseConf(environment: Environment): DatabaseConf = {
    val tables = environment.tables match {
      case Some(t) => Some(t.toSet)
      case None    => None
    }

    new DatabaseConf(
      databaseType = DatabaseType.withName(environment.databaseType),
      schema = environment.schema,
      jdbcUrl = environment.jdbcUrl,
      username = environment.username,
      password = environment.password,
      objectType = ObjectType.withName(environment.objectType),
      tables = tables
    )
  }
}