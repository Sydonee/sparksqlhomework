package com.epam.training.spark.sql

import org.apache.spark.sql.types._

/**
  * Created by Levente_Hortobagyi on 2017-04-21.
  */
object Constants {

  val CLIMATE_TYPE = StructType(
    StructField("observation_date", DateType, nullable = false) ::
      StructField("mean_temperature", DoubleType, nullable = false) ::
      StructField("max_temperature", DoubleType, nullable = false) ::
      StructField("min_temperature", DoubleType, nullable = false) ::
      StructField("precipitation_mm", DoubleType, nullable = true) ::
      StructField("precipitation_type", IntegerType, nullable = true) ::
      StructField("sunshine_hours", DoubleType, nullable = true) ::
      Nil)

}
