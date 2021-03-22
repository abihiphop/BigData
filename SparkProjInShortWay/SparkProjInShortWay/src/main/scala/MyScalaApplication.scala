

package com.mypkg

import org.apache.log4j.{Level, LogManager}
import org.apache.spark._
import org.apache.spark.rdd.RDD

object MyScalaApplication extends App {

  case class Car(Category:Int, Mean:Float)
  case class Result(Category:Int, Mean:Float, Variance:Float)

  def parseLine(line: String) = {
    val fields = line.split(",")
    val cyl = fields(2).toInt
    val mpg = fields(1).toFloat
    (cyl,mpg)
  }

  def computeMean(arr: Iterable[Float]) = {
    val count = arr.count(_ => true)
    val total = arr.sum
    total/count
  }

  def computeVariance(arr: Iterable[Float]) = {
    val cnt = arr.count(_ => true)
    val total = arr.map(x=>x*x).sum
    val mean = computeMean(arr)
    val sqMean = total/cnt
    sqMean-(mean*mean)
  }

  override def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Spark Project").setMaster("local")
    val sc = new SparkContext(conf)
    LogManager.getRootLogger.setLevel(Level.ERROR)
    println("\n\n\n\n\n\n------------------------------------")
    println("Step 1. Load input/mtcars.csv file.")
    val lines = sc.textFile("input/mtcars.csv")

    val header = lines.first()
    val rdd = lines.filter(line => line != header)
      .map(parseLine).cache()
    println("\nStep 2. Create a pair population \n(cyl,mpg)")
    rdd.sortBy(_._1).foreach(x => println(x._1, x._2))

    println("\nStep 3. Compute the mean mpg and variance for every cyl \n(cyl,mean,variance)")
    computeResult(rdd)

    println("\nStep 4. Create the sample for bootstrapping. Take 25% of the population without replacement. \n(cyl,mean,variance)")
    val sample = rdd.sample(false, 0.25)
    computeResult(sample)

    val doTimes = 200
    println("\nStep 5. Do " + doTimes + " times create the sample for bootstrapping. Take 100% of the population with replacement. \n(cyl,mean,variance)")
    var resampledData = List[org.apache.spark.rdd.RDD[(Int, Float)]]()
    for( i <- 1 to doTimes) {
      val resample = sample.sample(true, 1)
      resampledData = resample::resampledData
    }
    computeResult(resampledData.reduce(_ union _))
  }

  def computeResult(rdd: RDD[(Int, Float)]) = {
    rdd.groupBy(_._1).map(x => (x._1, computeMean(x._2.map(_._2)), computeVariance(x._2.map(_._2)))).foreach(println)
  }
}
