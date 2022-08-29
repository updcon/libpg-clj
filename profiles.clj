{:dev      {:plugins []}
 :provided {:dependencies [[org.clojure/clojure "1.10.3"]
                           ;; Logger
                           [com.fzakaria/slf4j-timbre "0.3.21"]
                           [org.slf4j/slf4j-api "1.7.33"]
                           [org.slf4j/log4j-over-slf4j "1.7.33"]
                           [org.slf4j/jul-to-slf4j "1.7.33"]
                           [org.slf4j/jcl-over-slf4j "1.7.33"]
                           [ch.qos.logback/logback-classic "1.2.10"
                            :exclusions [org.slf4j/slf4j-api]]
                           [com.taoensso/timbre "5.1.2"]]}
 :jar      {:aot :all}}
