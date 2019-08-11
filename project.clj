(defproject com.dkdhub/libpg-clj "0.0.3"
  :description "DKD commons for PostgreSQL"
  :url "https://dkdhub.com"
  :license {:name "Proprietary"
            :url  "https://dkdhub.com/licenses/base.html"}
  :main libpg-clj.core
  :omit-source true
  :dependencies [[org.clojure/java.jdbc "0.7.9"]
                 ;; PostgreSQL
                 [org.postgresql/postgresql "42.2.6"]
                 [nilenso/honeysql-postgres "0.2.6"]
                 [com.mchange/c3p0 "0.9.5.4"]
                 ;; Data helpers
                 [danlentz/clj-uuid "0.1.9"]
                 [cheshire "5.9.0"]
                 [joda-time "2.10.3"]])

(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(cemerick.pomegranate.aether/register-wagon-factory!
  "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
           (clojure.lang.Reflector/invokeConstructor c (into-array []))))
