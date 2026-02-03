(ns libpg-clj.debug
  "Debug utilities for PostgreSQL query inspection and analysis."
  (:require [clojure.java.jdbc :as jdbc]))

;; -- helpers for query debugging

(defn print-statement
  "Prints a prepared statement with bound parameters for debugging.

  Parameters:
    pool  - Database connection pool
    query - Query vector [sql & params]

  Prints the PreparedStatement object via pprint, showing the SQL
  with parameters bound."
  [pool [sql & params]]
  (doto (jdbc/prepare-statement (jdbc/get-connection pool) sql)
    (#'jdbc/dft-set-parameters params)
    clojure.pprint/pprint))

(defn query-explain
  "Executes EXPLAIN ANALYZE on a query and pretty-prints the execution plan.

  Parameters:
    pool  - Database connection pool
    query - Query vector [sql & params]

  Useful for analyzing query performance and understanding how PostgreSQL
  executes your queries."
  [pool query]
  (jdbc/query pool query {:explain?   "EXPLAIN ANALYZE"
                          :explain-fn clojure.pprint/pprint}))
