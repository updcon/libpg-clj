(ns libpg-clj.core
  (:gen-class)
  (:require [honeysql.format :as hfmt]
            [clojure.java.jdbc :refer [IResultSetReadColumn]]
            [cheshire.core :as json]
            [clj-uuid :as uuid])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)
           (org.postgresql.util PGobject)))

(defn make-pool [config]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname config))
               (.setJdbcUrl (format "jdbc:%s:%s?prepareThreshold=0"
                                    (:subprotocol config)
                                    (:subname config)))
               (.setUser (:user config))
               (.setPassword (:password config))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60))
               (.setInitialPoolSize (:min-pool config))
               (.setMinPoolSize (:min-pool config))
               (.setMaxPoolSize (:max-pool config)))]
    {:datasource cpds}))

;; -- helpers
(defn convert-enum [tk T p]
  (or (when-let [g (get p tk)]
        (assoc p tk [g (get T tk)]))
      p))

(defmacro add-time-label
  [p label]
  `(assoc ~p ~label (uuid/v1)))

(defmacro drop-fields
  [p & fields]
  `(dissoc ~p ~@fields))

(defmethod hfmt/fn-handler "ilike" [_ col qstr]
  (str (hfmt/to-sql col) " ILIKE " (hfmt/to-sql qstr)))

(extend-protocol IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _ _]
    (let [type (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (json/parse-string value true)
        "jsonb" (json/parse-string value true)
        value))))
