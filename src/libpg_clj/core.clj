(ns libpg-clj.core
  (:gen-class)
  (:require [honeysql.format :as hfmt]
            [honeysql.core :as sql :refer [raw call]]
            [clojure.java.jdbc :refer [execute! query IResultSetReadColumn]]
            [cheshire.core :as json]
            [clj-uuid :as uuid]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)
           (org.postgresql.util PGobject)
           (clojure.lang Keyword)))

(defn make-pool [config]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname config))
               (.setJdbcUrl (clojure.core/format
                              "jdbc:%s:%s?prepareThreshold=0"
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

(defn jdbc-exec [conn query & [_]]
  (first (execute! conn query)))

(defmacro h-cast
  [value type]
  `(call :cast ~value ~type))

(defn explain-enum [conn name]
  (->> [nil (keyword name)]
       (apply call :cast)
       (call :enum_range)
       (call :unnest)
       (assoc {} :select)
       (apply sql/format)
       (query conn)
       (mapv :unnest)))

;; -- extend types

(defn kw->pgenum [kw]
  (let [type (-> (namespace kw)
                 (str/replace "-" "_"))
        value (name kw)]
    (doto (PGobject.)
      (.setType type)
      (.setValue value))))

(extend-type Keyword
  jdbc/ISQLValue
  (sql-value [kw]
    (kw->pgenum kw)))

;; -- extend protocol

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

(defmethod hfmt/fn-handler "contains" [_ left right]
  (str (hfmt/to-sql left) " @> " (hfmt/to-sql right)))

(defmethod hfmt/fn-handler "array-exists" [_ left right]
  (str (hfmt/to-sql left) " ?? " (hfmt/to-sql right)))

(defmethod hfmt/fn-handler "array-exists-any" [_ left right]
  (str (hfmt/to-sql left) " ??| " (hfmt/to-sql right)))

(defmethod hfmt/fn-handler "cast" [_ value type]
  (str (hfmt/to-sql value) "::" (hfmt/to-sql type)))

(defmethod hfmt/fn-handler "->" [_ field key]
  (clojure.core/format "(%s::jsonb->'%s')" (hfmt/to-sql field) (name key)))

(defmethod hfmt/fn-handler "->>" [_ field key]
  (clojure.core/format "(%s::jsonb->>'%s')" (hfmt/to-sql field) (name key)))

(defmethod hfmt/fn-handler "#>" [_ field path]
  (clojure.core/format "(%s::jsonb#>'{%s}')"
                       (hfmt/to-sql field)
                       (str/join "," (map name path))))

(defmethod hfmt/fn-handler "#>>" [_ field path]
  (clojure.core/format "(%s::jsonb#>>'{%s}')"
                       (hfmt/to-sql field)
                       (str/join "," (map name path))))

(declare print-statement query-explain)

(defn json>
  ([field path]
   (call (if (vector? path) :#> :->) field path))
  ([field path type]
   (h-cast (json> field path) type)))

(defn json>>
  ([field path]
   (call (if (vector? path) :#>> :->>) field path))
  ([field path type]
   (h-cast (json>> field path) type)))

(def total (raw "COUNT(*) OVER ()"))

(defn json-agg [inner-select]
  {:select [(call :json_agg :x)]
   :from   [[inner-select :x]]})

(comment
  "Helpers for query debugging"
  (defn print-statement [pool [sql & params]]
    (doto (jdbc/prepare-statement (jdbc/get-connection pool) sql)
      (#'jdbc/dft-set-parameters params)
      clojure.pprint/pprint))

  (defn query-explain [pool query]
    (jdbc/query pool query {:explain?   "EXPLAIN ANALYZE"
                            :explain-fn clojure.pprint/pprint})))
