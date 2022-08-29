(defproject com.dkdhub/libpg-clj "0.1.0"
  :description "DKD commons for PostgreSQL"
  :url "https://dkdhub.com"
  :license {:name "Proprietary"
            :url  "https://dkdhub.com/licenses/base.html"}
  :dependencies [[org.clojure/java.jdbc "0.7.12"]
                 ;; PostgreSQL
                 [org.postgresql/postgresql "42.5.0"]
                 [nilenso/honeysql-postgres "0.4.112"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 ;; Data helpers
                 [danlentz/clj-uuid "0.1.9"]
                 [cheshire "5.11.0"]
                 [joda-time "2.11.1"]])

(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(cemerick.pomegranate.aether/register-wagon-factory!
  "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
           (clojure.lang.Reflector/invokeConstructor c (into-array []))))
