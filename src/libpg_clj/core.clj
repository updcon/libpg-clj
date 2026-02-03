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
           (clojure.lang Keyword)
           (java.io Closeable)))

;; -- c3p0 pool

(defrecord ConnectionPool [datasource]
  Closeable
  (close [_]
    (.close datasource)))

(defn make-pool
  "Creates a c3p0 connection pool for PostgreSQL.

  Config map keys:
    :classname         - JDBC driver class (e.g., \"org.postgresql.Driver\")
    :subprotocol       - Database subprotocol (e.g., \"postgresql\")
    :subname           - Database subname (e.g., \"//localhost:5432/mydb\")
    :user              - Database username
    :password          - Database password
    :min-pool          - Minimum/initial pool size
    :max-pool          - Maximum pool size
    :prepare-threshold - PreparedStatement threshold (default 0)

  Returns a ConnectionPool record (implements Closeable) with :datasource key,
  suitable for use with clojure.java.jdbc and with-open.

  Example:
    (with-open [pool (make-pool config)]
      (jdbc/query pool [\"SELECT 1\"]))"
  [config]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname config))
               (.setJdbcUrl (clojure.core/format
                              "jdbc:%s:%s?prepareThreshold=%d"
                              (:subprotocol config)
                              (:subname config)
                              (get config :prepare-threshold 0)))
               (.setUser (:user config))
               (.setPassword (:password config))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60))
               (.setInitialPoolSize (:min-pool config))
               (.setMinPoolSize (:min-pool config))
               (.setMaxPoolSize (:max-pool config)))]
    (->ConnectionPool cpds)))

(defn close-pool
  "Closes a connection pool, releasing all resources.

  Parameters:
    pool - A ConnectionPool created by make-pool

  Can also use (.close pool) or (with-open [pool (make-pool config)] ...)."
  [pool]
  (.close pool))

;; -- helpers

(defn convert-enum
  "Converts a field in map p to a [value type] pair for enum casting.

  Parameters:
    tk - The key to convert in the map
    T  - A type map where (get T tk) returns the enum type name
    p  - The map containing the value to convert

  Returns the map with tk's value converted to [value type] format,
  or the original map if tk is not present."
  [tk T p]
  (or (when-let [g (get p tk)]
        (assoc p tk [g (get T tk)]))
      p))

(defmacro add-time-label
  "Adds a UUID v1 timestamp label to a map.

  Example:
    (add-time-label {:name \"test\"} :created-at)
    ;=> {:name \"test\" :created-at #uuid \"...\"}"
  [p label]
  `(assoc ~p ~label (uuid/v1)))

(defmacro drop-fields
  "Removes specified fields from a map. Convenience wrapper around dissoc.

  Example:
    (drop-fields {:a 1 :b 2 :c 3} :a :c)
    ;=> {:b 2}"
  [p & fields]
  `(dissoc ~p ~@fields))

(defn jdbc-exec
  "Executes a JDBC query and returns the first result.

  Parameters:
    conn  - Database connection or pool
    query - SQL query vector [sql & params]

  Returns the first element of the execute! result (typically row count)."
  [conn query & [_]]
  (first (execute! conn query)))

(defmacro h-cast
  "Creates a HoneySQL cast expression.

  Example:
    (h-cast :my-field :integer)
    ;=> Generates SQL: my_field::integer"
  [value type]
  `(call :cast ~value ~type))

(defn explain-enum
  "Retrieves all possible values for a PostgreSQL enum type.

  Parameters:
    conn - Database connection or pool
    name - The enum type name as a string

  Returns a vector of enum values as strings."
  [conn name]
  (->> [nil (keyword name)]
       (apply call :cast)
       (call :enum_range)
       (call :unnest)
       (assoc {} :select)
       (apply sql/format)
       (query conn)
       (mapv :unnest)))

;; -- extend types

(defn kw->pgenum
  "Converts a namespaced keyword to a PostgreSQL enum PGobject.

  The keyword namespace becomes the enum type (with - replaced by _),
  and the keyword name becomes the enum value.

  Example:
    (kw->pgenum :user-status/active)
    ;=> PGobject with type=\"user_status\" value=\"active\""
  [kw]
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

(defn json>
  "JSONB path accessor returning JSON (preserves type).

  Uses -> for single key access, #> for path (vector) access.

  Parameters:
    field - The JSONB column name
    path  - Key (keyword) or path (vector of keywords)
    type  - Optional: cast result to this type

  Examples:
    (json> :data :name)           ; data::jsonb->'name'
    (json> :data [:user :name])   ; data::jsonb#>'{user,name}'
    (json> :data :age :integer)   ; (data::jsonb->'age')::integer"
  ([field path]
   (call (if (vector? path) :#> :->) field path))
  ([field path type]
   (h-cast (json> field path) type)))

(defn json>>
  "JSONB path accessor returning text (always string).

  Uses ->> for single key access, #>> for path (vector) access.

  Parameters:
    field - The JSONB column name
    path  - Key (keyword) or path (vector of keywords)
    type  - Optional: cast result to this type

  Examples:
    (json>> :data :name)           ; data::jsonb->>'name'
    (json>> :data [:user :name])   ; data::jsonb#>>'{user,name}'
    (json>> :data :age :integer)   ; (data::jsonb->>'age')::integer"
  ([field path]
   (call (if (vector? path) :#>> :->>) field path))
  ([field path type]
   (h-cast (json>> field path) type)))

(def total
  "A raw SQL expression for counting total rows using a window function.
  Useful for pagination queries to get total count alongside results.

  Usage in HoneySQL:
    {:select [:id :name total] :from [:users]}"
  (raw "COUNT(*) OVER ()"))

(defn json-agg
  "Wraps a subquery with JSON aggregation.

  Creates a query that aggregates all rows from inner-select into a JSON array.

  Parameters:
    inner-select - A HoneySQL query map

  Example:
    (json-agg {:select [:id :name] :from [:users]})
    ;=> {:select [(call :json_agg :x)] :from [[{...} :x]]}"
  [inner-select]
  {:select [(call :json_agg :x)]
   :from   [[inner-select :x]]})
