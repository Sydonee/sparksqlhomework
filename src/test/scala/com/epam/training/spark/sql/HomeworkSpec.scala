package com.epam.training.spark.sql

import java.time.LocalDate

import com.epam.hubd.spark.scala.core.util.RddComparator
import org.apache.spark.sql.Row
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, FunSpec}

import scala.collection.JavaConverters._

class HomeworkSpec extends FunSpec with BeforeAndAfterAll {

  val INPUT_BIDS_INTEGRATION = "src/test/resources/integration/input/input.csv"

  val sparkConf: SparkConf = new SparkConf()
    .setAppName("EPAM BigData training Spark SQL homework test")
    .setIfMissing("spark.master", "local[2]")
    .setIfMissing("spark.sql.shuffle.partitions", "2")
  val sc = new SparkContext(sparkConf)
  val sqlContext = new HiveContext(sc)

  val CLIMATE_DATA = List(
    Row("1901-01-01", "-11.7", "-9.0", "-13.6", "2.2", "4", ""),
    Row("1901-01-02", "-11.1", "-10.0", "-15.2", "3.0", "4", ""),
    Row("1901-01-03", "-13.0", "-10.0", "-15.7", "", "", ""),
    Row("1955-01-01", "-2.1", "-1.0", "-3.4", "2.5", "4", "0.6"),
    Row("1955-01-02", "-2.3", "-1.5", "-3.1", "0.1", "4", "0.1"),
    Row("1955-01-03", "-3.9", "-2.0", "-6.5", "5.3", "4", "0.0"),
    Row("2010-01-01", "5.0", "7.8", "3.6", "", "", "0.7"),
    Row("2010-01-02", "1.6", "5.8", "0.1", "0.1", "4", "1.4"),
    Row("2010-01-03", "-1.9", "0.2", "-2.6", "0.0", "4", "0.5")
  )

  val CLIMATE_DATAFRAME = List(
    Row(java.sql.Date.valueOf(LocalDate.of(1901, 1, 1)), -11.7, -9.0, -13.6, 2.2, 4, null),
    Row(java.sql.Date.valueOf(LocalDate.of(1901, 1, 2)), -11.1, -10.0, -15.2, 3.0, 4, null),
    Row(java.sql.Date.valueOf(LocalDate.of(1901, 1, 3)), -13.0, -10.0, -15.7, null, null, null),
    Row(java.sql.Date.valueOf(LocalDate.of(1955, 1, 1)), -2.1, -1.0, -3.4, 2.5, 4, 0.6),
    Row(java.sql.Date.valueOf(LocalDate.of(1955, 1, 2)), -2.3, -1.5, -3.1, 0.1, 4, 0.1),
    Row(java.sql.Date.valueOf(LocalDate.of(1955, 1, 3)), -3.9, -2.0, -6.5, 5.3, 4, 0.0),
    Row(java.sql.Date.valueOf(LocalDate.of(2010, 1, 1)), 5.0, 7.8, 3.6, null, null, 0.7),
    Row(java.sql.Date.valueOf(LocalDate.of(2010, 1, 2)), 1.6, 5.8, 0.1, 0.1, 4, 1.4),
    Row(java.sql.Date.valueOf(LocalDate.of(2010, 1, 3)), -1.9, 0.2, -2.6, 0.0, 4, 0.5)
  )

  override def afterAll() {
    sc.stop()
  }

  describe("dataframe") {
    describe("when reading from csv file") {
      it("should have a list of Rows") {
        val actual = Homework.readCsvData(sqlContext, INPUT_BIDS_INTEGRATION)
        val expected = CLIMATE_DATAFRAME
        RddComparator.printRowDiff(expected.toArray, actual.collect())
        assert(actual.collect() === expected)
      }
    }

    describe("when counting missing data") {
      it("should summarize them") {
        val actual = Homework.findErrors(sqlContext.createDataFrame(CLIMATE_DATAFRAME.asJava, Constants.CLIMATE_TYPE))
        val expected = Array(Row(0, 0, 0, 0, 2, 2, 3))
        assert(actual === expected)
      }
    }

    describe("when checking temperature for a given day") {
      it("it should contain the temperatures for all observed dates") {
        val actual = Homework.averageTemperature(sqlContext.createDataFrame(CLIMATE_DATAFRAME.asJava, Constants.CLIMATE_TYPE), 1, 2)
        val expected = Array(Row(-11.1), Row(-2.3), Row(1.6))
        RddComparator.printRowDiff(expected, actual.collect())
        assert(actual.collect() === expected)
      }
    }

    describe("when predicting temperature for a given day") {
      it("it should return a temperature based on all observed dates including previous and next days") {
        val actual = Homework.predictTemperature(sqlContext.createDataFrame(CLIMATE_DATAFRAME.asJava, Constants.CLIMATE_TYPE), 1, 2)
        val expected = -4.377777777777776
        assert(actual === expected)
        //assert(actual === expected +- 0.0001)
      }
    }

  }


}
