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

package io.phdata.jdbc.parsing

import java.sql.{Connection, ResultSetMetaData}

import com.typesafe.scalalogging.LazyLogging
import io.phdata.jdbc.domain.Column

import scala.util.{Failure, Success, Try}

/**
  * MySQL metadata parser implementation
  * @param _connection
  */
class MySQLMetadataParser(_connection: Connection) extends DatabaseMetadataParser {

  def connection = _connection

  override def singleRecordQuery(schema: String, table: String) =
    s"""
       |SELECT *
       |FROM $schema.$table
       |LIMIT 1
     """.stripMargin

  override def listTablesStatement(schema: String) =
    s"""
       |SELECT TABLE_NAME
       |FROM INFORMATION_SCHEMA.TABLES
       |WHERE TABLE_SCHEMA = '$schema' AND TABLE_TYPE = 'BASE TABLE'
     """.stripMargin


  override def listViewsStatement(schema: String): String =
    s"""
       |SELECT TABLE_NAME
       |FROM INFORMATION_SCHEMA.VIEWS
       |WHERE TABLE_SCHEMA = '$schema'
     """.stripMargin

  override def getColumnDefinitions(schema: String,
                                    table: String): Try[Set[Column]] = {
    val query = singleRecordQuery(schema, table)
    logger.debug(s"Gathering column definitions for $schema.$table, query: {}", query)
    results(newStatement.executeQuery(query))(_.getMetaData).toList.headOption match {
      case Some(metaData) =>
        val rsMetadata = metaData.asInstanceOf[com.mysql.cj.jdbc.result.ResultSetMetaData]
        Success(mapMetaDataToColumn(metaData, rsMetadata))
      case None =>
        Failure(new Exception(s"$table does not contain any records, cannot provide column definitions"))
    }

  }
}
