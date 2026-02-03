(defproject ai.z7/libpg-clj "0.1.0"
  :description "DKD commons for PostgreSQL in Clojure"
  :url "https://dkdhub.com"
  :license {:name "MIT"
            :url "https://github.com/updcon/libpg-clj"}
  :dependencies [[org.clojure/java.jdbc "0.7.12"]
                 ;; PostgreSQL
                 [org.postgresql/postgresql "42.7.9"]
                 [nilenso/honeysql-postgres "0.4.112"]
                 [com.mchange/c3p0 "0.11.2"]
                 ;; Data helpers
                 [danlentz/clj-uuid "0.2.0"]
                 [cheshire "6.1.0"]
                 [joda-time "2.14.0"]])
