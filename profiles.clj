{:dev      {:plugins             [[lein-ancient "0.6.15"]
                                  [org.apache.maven.wagon/wagon-http-lightweight "3.0.0"]]
            :deploy-repositories [["private-jars" "http://local.repo:9180/repo"]]}
 :provided {:dependencies [[org.clojure/clojure "1.9.0"]
                           ;; Logger
                           [ch.qos.logback/logback-classic "1.2.3"
                            :exclusions [org.slf4j/slf4j-api]]
                           [org.slf4j/jul-to-slf4j "1.7.25"]
                           [org.slf4j/jcl-over-slf4j "1.7.25"]
                           [org.slf4j/log4j-over-slf4j "1.7.25"]
                           [org.clojure/tools.logging "0.4.0"]]}
 :uberjar  {:aot :all :jvm-opts #=(eval
                                    (concat ["-Xmx1G"]
                                      (let [version (System/getProperty "java.version")
                                            [major _ _] (clojure.string/split version #"\.")]
                                        (if (>= (Integer. major) 9)
                                          ["--add-modules" "java.xml.bind"]
                                          []))))}}
