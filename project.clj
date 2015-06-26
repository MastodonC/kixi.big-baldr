(defproject kixi.big-baldr "0.1.0"
  :description "Parse baldr format files for hadoop 2.4 and spark 1.3"
  :url "http://github.com/mastodonc/kixi.big-baldr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["java/src" "java/test"]
  :javac-options ["-source" "1.7" "-target" "1.7"]
  :dependencies [[baldr "0.1.1"]]
  :profiles {:dev
             {:dependencies [[criterium "0.4.3"]
                             [junit "4.11"]]}
             :provided
             {:dependencies
              [[junit "4.11"]
               [org.apache.hadoop/hadoop-client "2.4.0"]
               [org.apache.spark/spark-core_2.10 "1.3.0"]
               [org.apache.spark/spark-streaming_2.10 "1.3.0"]
               [org.apache.spark/spark-streaming-kafka_2.10 "1.3.0"]
               [org.apache.spark/spark-streaming-flume_2.10 "1.3.0"]
               [org.apache.spark/spark-sql_2.10 "1.3.0"]]}
             :uberjar
             {:aot :all}})
