package org.training.spark.sql

import org.apache.spark._
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.Row

/**
 * 看过“Lord of the Rings, The (1978)”用户年龄和性别分布
 */
object MovieUserAnalyzerWithDataFrame {
  def main(args: Array[String]) {
    var masterUrl = "local[1]"
    var dataPath = "data/ml-1m/"
    if (args.length > 0) {
      masterUrl = args(0)
    } else if(args.length > 1) {
      dataPath = args(1)
    }

    // Create a SparContext with the given master URL
    val conf = new SparkConf().setAppName("MovieUserAnalyzerWithDataFrame")
        conf.setMaster("spark://master:7077"); //master URL

    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)

    /**
     * Step 1: Create RDDs
     */
    val DATA_PATH = dataPath
    val MOVIE_TITLE = "Lord of the Rings, The (1978)"
    val MOVIE_ID = "2116"

    val usersRdd = sc.textFile(DATA_PATH + "users.dat")
    val ratingsRdd = sc.textFile(DATA_PATH + "ratings.dat")

    /**
     * Step 2: Transform to DataFrame
     */
    import org.apache.spark.sql.types.{StructType,StructField,StringType}

    val userSchemaString = "userid gender age"
    val schema = StructType(userSchemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))
    val userRDD = usersRdd.map(_.split("::")).map(p => Row(p(0), p(1).trim, p(2).trim))
    val userDataFrame = sqlContext.createDataFrame(userRDD, schema)

    val ratingSchemaString = "userid movieid"
    val ratingSchema = StructType(ratingSchemaString.split(" ")
        .map(fieldName => StructField(fieldName, StringType, true)))
    val ratingRDD = ratingsRdd.map(_.split("::")).map(p => Row(p(0), p(1).trim))
    val ratingDataFrame = sqlContext.createDataFrame(ratingRDD, ratingSchema)

    /**
     * Step 3: process & print result
     */

    // use rdd
    ratingDataFrame.filter(s"movieid = ${MOVIE_ID}").
        join(userDataFrame, "userid").
        select("gender", "age").
        groupBy("gender", "age").
        count().
        foreach(println)

    // use dataframe
    userDataFrame.registerTempTable("users")
    ratingDataFrame.registerTempTable("rating")
    sqlContext.sql("SELECT gender, age, count(*) from users as u join rating  as r " +
       s"on u.userid = r.userid where movieid = ${MOVIE_ID} group by gender, age").
        foreach(println)

    sc.stop()
  }
}
