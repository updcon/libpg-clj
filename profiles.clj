{:dev      {:plugins             [[lein-ancient "0.6.15"]
                                  [org.apache.maven.wagon/wagon-ssh-external "3.0.0"]
                                  [org.apache.maven.wagon/wagon-http-lightweight "3.0.0"]]
            :deploy-repositories [["private-jars-scp" {:url              "scp://local.repo/home/clojar/data/dev_repo/"
                                                       :username         "clojar"
                                                       :private-key-file :env/clojure_ssh_key}]]}
 :provided {:dependencies [[org.clojure/clojure "1.10.0"]
                           ;; Logger
                           [com.fzakaria/slf4j-timbre "0.3.12"]
                           [org.slf4j/slf4j-api "1.7.25"]
                           [org.slf4j/log4j-over-slf4j "1.7.25"]
                           [org.slf4j/jul-to-slf4j "1.7.25"]
                           [org.slf4j/jcl-over-slf4j "1.7.25"]
                           [ch.qos.logback/logback-classic "1.2.3"
                            :exclusions [org.slf4j/slf4j-api]]
                           [com.taoensso/timbre "4.10.0"]]}
 :uberjar  {:aot :all :jvm-opts #=(eval
                                    (concat ["-Xmx1G"]
                                      (let [version (System/getProperty "java.version")
                                            [major _ _] (clojure.string/split version #"\.")]
                                        (if (>= (Integer. major) 9)
                                          ["--add-modules" "java.xml.bind"]
                                          []))))}}
