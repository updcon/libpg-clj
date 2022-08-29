(defproject ai.z7/libpg-clj "0.1.0"
  :description "DKD commons for PostgreSQL in Clojure"
  :url "https://dkdhub.com"
  :license {:name "MIT"
            :url "https://github.com/updcon/libpg-clj"}
  :dependencies [[org.clojure/java.jdbc "0.7.12"]
                 ;; PostgreSQL
                 [org.postgresql/postgresql "42.5.0"]
                 [nilenso/honeysql-postgres "0.4.112"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 ;; Data helpers
                 [danlentz/clj-uuid "0.1.9"]
                 [cheshire "5.11.0"]
                 [joda-time "2.11.1"]])
