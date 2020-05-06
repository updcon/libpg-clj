{:dev      {:plugins             [[org.apache.maven.wagon/wagon-ssh-external "3.4.0"]
                                  [org.apache.maven.wagon/wagon-http-lightweight "3.4.0"]]
            :deploy-repositories [["private-jars-scp" {:url              "scp://local.repo/home/clojar/data/dev_repo/"
                                                       :username         "clojar"
                                                       :private-key-file :env/clojure_ssh_key}]]}
 :provided {:dependencies [[org.clojure/clojure "1.10.1"]
                           ;; Logger
                           [com.fzakaria/slf4j-timbre "0.3.19"]
                           [org.slf4j/slf4j-api "1.7.30"]
                           [org.slf4j/log4j-over-slf4j "1.7.30"]
                           [org.slf4j/jul-to-slf4j "1.7.30"]
                           [org.slf4j/jcl-over-slf4j "1.7.30"]
                           [ch.qos.logback/logback-classic "1.2.3"
                            :exclusions [org.slf4j/slf4j-api]]
                           [com.taoensso/timbre "4.10.0"]]}
 :jar      {:aot :all}}
