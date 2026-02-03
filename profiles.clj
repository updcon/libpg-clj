{:dev      {:plugins []}
 :provided {:dependencies [[org.clojure/clojure "1.12.4"]
                           ;; Logger
                           [com.fzakaria/slf4j-timbre "0.4.1"]
                           [org.slf4j/slf4j-api "2.0.17"]
                           [org.slf4j/log4j-over-slf4j "2.0.17"]
                           [org.slf4j/jul-to-slf4j "2.0.17"]
                           [org.slf4j/jcl-over-slf4j "2.0.17"]
                           [ch.qos.logback/logback-classic "1.5.27"
                            :exclusions [org.slf4j/slf4j-api]]
                           [com.taoensso/timbre "6.8.0"]]}
 :jar      {:aot :all}}
