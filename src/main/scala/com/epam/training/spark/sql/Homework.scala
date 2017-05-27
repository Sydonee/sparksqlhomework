package com.epam.training.spark.sql

import java.sql.Date

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.{SparkConf, SparkContext};

object Homework {
  val DELIMITER = ";"
  val RAW_BUDAPEST_DATA = "data/budapest_daily_1901-2010.csv"
  val OUTPUT_DUR = "output"

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("EPAM BigData training Spark SQL homework")
      .setIfMissing("spark.master", "local[2]")
      .setIfMissing("spark.sql.shuffle.partitions", "10")
    val sc = new SparkContext(sparkConf)
    val sqlContext = new HiveContext(sc)

    processData(sqlContext)

    sc.stop()

  }

  def processData(sqlContext: HiveContext): Unit = {

    /**
      * Task 1
      * Read csv data with DataSource API from provided file
      * Hint: schema is in the Constants object
      */
    val climateDataFrame: DataFrame = readCsvData(sqlContext, Homework.RAW_BUDAPEST_DATA)

    /**
      * Task 2
      * Find errors or missing values in the data
      * Hint: try to use udf for the null check
      */
    val errors: Array[Row] = findErrors(climateDataFrame)
    println(errors)

    /**
      * Task 3
      * List average temperature for a given day in every year
      */
    val averageTemeperatureDataFrame: DataFrame = averageTemperature(climateDataFrame, 1, 2)

    /**
      * Task 4
      * Predict temperature based on mean temperature for every year including 1 day before and after
      * For the given month 1 and day 2 (2nd January) include days 1st January and 3rd January in the calculation
      * Hint: if the dataframe contains a single row with a single double value you can get the double like this "df.first().getDouble(0)"
      */
    val predictedTemperature: Double = predictTemperature(climateDataFrame, 1, 2)
    println(s"Predicted temperature: $predictedTemperature")

  }

  def isFieldValid(line: Array[String], fieldIndex: Int):Boolean =  {
    return (line.length>fieldIndex && line(fieldIndex)!="")
  }

  def readCsvData(sqlContext: HiveContext, rawDataPath: String): DataFrame = {
    val allRowsRDD =sqlContext.sparkContext.textFile(rawDataPath);
    val firstRow = allRowsRDD.first();
    val dataRowsRDD =  allRowsRDD.filter(r=>r!=firstRow);
    val climateInfoRDD: RDD[Row] =
      dataRowsRDD
        .map(_.split(";"))
        .map(line => Row (
          if (isFieldValid(line, 0)) Date.valueOf(line(0)) else null,
          if (isFieldValid(line, 1)) line(1).toDouble else null,
          if (isFieldValid(line, 2)) line(2).toDouble else null,
          if (isFieldValid(line, 3)) line(3).toDouble else null,
          if (isFieldValid(line, 4)) line(4).toDouble else null,
          if (isFieldValid(line, 5)) line(5).toInt else null,
          if (isFieldValid(line, 6)) line(6).toDouble else null));

    return sqlContext.createDataFrame(climateInfoRDD,Constants.CLIMATE_TYPE);
  }

  def findErrors(climateDataFrame: DataFrame): Array[Row] = {
    val cols= climateDataFrame.columns;
    var counts :Array[Int]= Array();
    cols.foreach(
      c=>  counts:+= climateDataFrame.where(c + " is null").count().toInt);

    return Array(Row(counts(0),counts(1),counts(2),counts(3),counts(4),counts(5),counts(6)));
  }

  def filterByDayAndMonth(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): DataFrame = {
    import climateDataFrame.sqlContext.implicits._;
    return climateDataFrame
      .filter(org.apache.spark.sql.functions.dayofmonth($"observation_date")===dayOfMonth)
      .filter(org.apache.spark.sql.functions.month($"observation_date")===monthNumber);
  }

  def filterByDayAndMonthWithPrevAndNext(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): DataFrame = {
    import climateDataFrame.sqlContext.implicits._;
    return filterByDayAndMonth(climateDataFrame,monthNumber, dayOfMonth)
      .select($"mean_temperature".alias("main_mean_temperature"),
        org.apache.spark.sql.functions.date_sub($"observation_date",1).alias("prev_day"),
        org.apache.spark.sql.functions.date_add($"observation_date",1).alias("next_day"));
  }

  def averageTemperature(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): DataFrame = {
    return filterByDayAndMonth(climateDataFrame,monthNumber, dayOfMonth)
      .select("mean_temperature");
  }

  def predictTemperature(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): Double = {
    import climateDataFrame.sqlContext.implicits._;

    val relevantDataOfGivenDay = filterByDayAndMonthWithPrevAndNext(climateDataFrame, monthNumber, dayOfMonth);

    val temperaturesOfPreviousDays = climateDataFrame.join(relevantDataOfGivenDay, climateDataFrame.col("observation_date")
      .equalTo(relevantDataOfGivenDay.col("prev_day"))).select("mean_temperature");

    val temperaturesOfNextDays = climateDataFrame.join(relevantDataOfGivenDay, climateDataFrame.col("observation_date")
      .equalTo(relevantDataOfGivenDay.col("next_day"))).select("mean_temperature");

    val rowsNeeded=relevantDataOfGivenDay.select($"main_mean_temperature".alias("mean_temperature"))
      .union(temperaturesOfPreviousDays)
      .union(temperaturesOfNextDays);

    return rowsNeeded.groupBy().avg("mean_temperature").first().getAs[Double](0);
  }


}


