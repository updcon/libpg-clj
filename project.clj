(defproject libpg-clj "0.0.1-SNAPSHOT"
  :description "A Clojure library designed to support..."
  :url "https://github.com/your-github-name/libpg-clj"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/java.jdbc "0.7.7"]
                 ;; PostgreSQL
                 [org.postgresql/postgresql "42.2.4"]
                 [nilenso/honeysql-postgres "0.2.4"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 ;; Data helpers
                 [danlentz/clj-uuid "0.1.7"]
                 [cheshire "5.8.0"]
                 [joda-time "2.10"]])

(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))
