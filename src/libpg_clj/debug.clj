(ns libpg-clj.debug
  (:require [clojure.java.jdbc :as jdbc]))

;; -- helpers for query debugging

(defn print-statement [pool [sql & params]]
  (doto (jdbc/prepare-statement (jdbc/get-connection pool) sql)
    (#'jdbc/dft-set-parameters params)
    clojure.pprint/pprint))

(defn query-explain [pool query]
  (jdbc/query pool query {:explain?   "EXPLAIN ANALYZE"
                          :explain-fn clojure.pprint/pprint}))
