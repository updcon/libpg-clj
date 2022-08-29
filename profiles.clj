{:dev      {:plugins []}
 :provided {:dependencies [[org.clojure/clojure "1.11.1"]
                           ;; Logger
                           [com.fzakaria/slf4j-timbre "0.3.21"]
                           [org.slf4j/slf4j-api "2.0.0"]
                           [org.slf4j/log4j-over-slf4j "2.0.0"]
                           [org.slf4j/jul-to-slf4j "2.0.0"]
                           [org.slf4j/jcl-over-slf4j "2.0.0"]
                           [ch.qos.logback/logback-classic "1.4.0"
                            :exclusions [org.slf4j/slf4j-api]]
                           [com.taoensso/timbre "5.2.1"]]}
 :jar      {:aot :all}}
