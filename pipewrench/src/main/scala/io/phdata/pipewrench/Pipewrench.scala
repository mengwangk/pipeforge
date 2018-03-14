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

package io.phdata.pipewrench

import java.io.FileWriter
import java.sql.JDBCType

import com.typesafe.scalalogging.LazyLogging
import io.phdata.pipeforge.jdbc.DatabaseMetadataParser
import io.phdata.pipeforge.jdbc.config.DatabaseConf
import io.phdata.pipeforge.jdbc.domain.{ DataType, Column => DbColumn, Table => DbTable }
import io.phdata.pipewrench.domain._
import net.jcazevedo.moultingyaml._

import scala.util.{ Failure, Success, Try }

trait Pipewrench {

  def buildConfiguration(databaseConf: DatabaseConf,
                         tableMetadata: Map[String, String],
                         environment: Environment,
                         impalaCmd: String = "impala-shell"): Try[Configuration]

}

object PipewrenchImpl extends Pipewrench with YamlSupport with LazyLogging {

  override def buildConfiguration(databaseConf: DatabaseConf,
                                  tableMetadata: Map[String, String],
                                  environment: Environment,
                                  impalaCmd: String): Try[Configuration] =
    DatabaseMetadataParser.parse(databaseConf) match {
      case Success(tables: Seq[DbTable]) =>
        logger.debug(s"Successfully parsed JDBC metadata: $tables")
        Try(
          Configuration(
            environment.name,
            environment.group,
            databaseConf.username,
            sqoop_password_file = environment.password_file,
            connection_manager = "",
            sqoop_job_name_suffix = environment.name,
            source_database = Map(
              "name" -> databaseConf.schema,
              "cmd" -> databaseConf.databaseType.toString),
            staging_database = Map(
              "path" -> environment.hdfs_basedir,
              "name" -> environment.destination_database
            ),
            impala_cmd = impalaCmd,
            tables = buildTables(tables, tableMetadata)
          )
        )
      case Failure(ex) =>
        logger.error("Failed to parse metadata config", ex)
        Failure(ex)
    }

  private def buildTables(tables: Seq[DbTable], tableMetadata: Map[String, String]): Seq[Table] =
    tables.toList
      .sortBy(_.name)
      .map { table =>
        logger.debug(s"Table definition: $table")
        val allColumns = table.primaryKeys ++ table.columns
        val pks = table.primaryKeys.toList.sortBy(_.index).map(_.name)
        Table(
          table.name,
          Map("name" -> table.name),
          Map("name" -> table.name),
          getSplitByColumn(table),
          pks,
          Kudu(pks, 2),
          buildColumns(allColumns),
          tableMetadata,
        )
      }

  private def buildColumns(columns: Set[DbColumn]): Seq[Column] =
    columns.toList
      .sortBy(_.index)
      .map { column =>
        val dataType = DataType.mapDataType(column)
        logger.debug(s"Column definition: $column, mapped dataType: $dataType")
        val columnYaml = Column(column.name, dataType)
        if (dataType == DataType.DECIMAL.toString) {
          logger.trace("Found decimal value: {}", column)
          columnYaml.copy(scale = Some(column.scale), precision = Some(column.precision))
        } else {
          columnYaml
        }
      }

  def getSplitByColumn(table: DbTable) = {
    val jdbc_numerics = List(JDBCType.BIGINT,
                             JDBCType.REAL,
                             JDBCType.DECIMAL,
                             JDBCType.DOUBLE,
                             JDBCType.FLOAT,
                             JDBCType.INTEGER,
                             JDBCType.NUMERIC)
    table.primaryKeys
      .find(x => { jdbc_numerics contains x.dataType })
      .orElse(table.primaryKeys.headOption)
      .orElse(table.columns.find(x => { jdbc_numerics contains x.dataType }))
      .orElse(table.columns.headOption)
      .get
      .name
  }

}