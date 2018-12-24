(defproject com.dkdhub/libpg-clj "0.0.2"
  :description "DKD commons for PostgreSQL"
  :url "https://dkdhub.com"
  :license {:name "Proprietary"
            :url  "https://dkdhub.com/licenses/base.html"}
  :main libpg-clj.core
  :omit-source true
  :dependencies [[org.clojure/java.jdbc "0.7.8"]
                 ;; PostgreSQL
                 [org.postgresql/postgresql "42.2.5"]
                 [nilenso/honeysql-postgres "0.2.5"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 ;; Data helpers
                 [danlentz/clj-uuid "0.1.7"]
                 [cheshire "5.8.1"]
                 [joda-time "2.10.1"]])

(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(cemerick.pomegranate.aether/register-wagon-factory!
  "scp" #(let [c (resolve 'org.apache.maven.wagon.providers.ssh.external.ScpExternalWagon)]
           (clojure.lang.Reflector/invokeConstructor c (into-array []))))
