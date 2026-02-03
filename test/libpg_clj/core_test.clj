(ns libpg-clj.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [libpg-clj.core :as core]
            [honeysql.core :as sql])
  (:import (org.postgresql.util PGobject)
           (java.io Closeable)))

;; -- Helper function tests

(deftest convert-enum-test
  (testing "converts existing key to [value type] pair"
    (let [type-map {:status "user_status"}
          input {:status "active" :name "test"}
          result (core/convert-enum :status type-map input)]
      (is (= ["active" "user_status"] (:status result)))
      (is (= "test" (:name result)))))

  (testing "returns original map when key not present"
    (let [type-map {:status "user_status"}
          input {:name "test"}
          result (core/convert-enum :status type-map input)]
      (is (= input result))))

  (testing "handles nil value in map"
    (let [type-map {:status "user_status"}
          input {:status nil :name "test"}
          result (core/convert-enum :status type-map input)]
      (is (= input result)))))

(deftest add-time-label-test
  (testing "adds UUID v1 to map"
    (let [input {:name "test"}
          result (core/add-time-label input :created-at)]
      (is (contains? result :created-at))
      (is (uuid? (:created-at result)))
      (is (= "test" (:name result))))))

(deftest drop-fields-test
  (testing "removes single field"
    (let [input {:a 1 :b 2 :c 3}
          result (core/drop-fields input :a)]
      (is (= {:b 2 :c 3} result))))

  (testing "removes multiple fields"
    (let [input {:a 1 :b 2 :c 3 :d 4}
          result (core/drop-fields input :a :c)]
      (is (= {:b 2 :d 4} result))))

  (testing "handles missing fields gracefully"
    (let [input {:a 1 :b 2}
          result (core/drop-fields input :x :y)]
      (is (= input result)))))

(deftest kw->pgenum-test
  (testing "creates PGobject from namespaced keyword"
    (let [result (core/kw->pgenum :user-status/active)]
      (is (instance? PGobject result))
      (is (= "user_status" (.getType result)))
      (is (= "active" (.getValue result)))))

  (testing "replaces hyphens with underscores in type"
    (let [result (core/kw->pgenum :my-custom-type/value)]
      (is (= "my_custom_type" (.getType result))))))

(deftest h-cast-test
  (testing "creates HoneySQL cast call"
    (let [result (core/h-cast :my-field :integer)
          [sql] (sql/format {:select [result]})]
      (is (string? sql))
      (is (re-find #"my.field::integer" sql)))))

;; -- HoneySQL operator tests

(deftest ilike-operator-test
  (testing "generates ILIKE SQL"
    (let [query {:select [:*]
                 :from [:users]
                 :where (sql/call :ilike :name "test%")}
          [sql & params] (sql/format query)]
      (is (re-find #"ILIKE" sql))
      (is (= "test%" (first params))))))

(deftest contains-operator-test
  (testing "generates @> containment SQL"
    (let [query {:select [:*]
                 :from [:users]
                 :where (sql/call :contains :tags "tag")}
          [sql] (sql/format query)]
      (is (re-find #"@>" sql)))))

(deftest array-exists-operator-test
  (testing "generates ?? array exists SQL"
    (let [query {:select [:*]
                 :from [:users]
                 :where (sql/call :array-exists :data "key")}
          [sql] (sql/format query)]
      (is (re-find #"\?\?" sql)))))

(deftest array-exists-any-operator-test
  (testing "generates ??| array exists any SQL"
    (let [query {:select [:*]
                 :from [:users]
                 :where (sql/call :array-exists-any :data "keys")}
          [sql] (sql/format query)]
      (is (re-find #"\?\?\|" sql)))))

(deftest cast-operator-test
  (testing "generates :: cast SQL"
    (let [query {:select [(sql/call :cast :value :integer)]}
          [sql] (sql/format query)]
      (is (re-find #"::integer" sql)))))

(deftest json-arrow-operator-test
  (testing "generates -> JSONB accessor SQL"
    (let [query {:select [(sql/call :-> :data :name)]}
          [sql] (sql/format query)]
      (is (re-find #"jsonb->'name'" sql)))))

(deftest json-double-arrow-operator-test
  (testing "generates ->> JSONB text accessor SQL"
    (let [query {:select [(sql/call :->> :data :name)]}
          [sql] (sql/format query)]
      (is (re-find #"jsonb->>'name'" sql)))))

(deftest json-path-operator-test
  (testing "generates #> JSONB path accessor SQL"
    (let [query {:select [(sql/call :#> :data [:user :name])]}
          [sql] (sql/format query)]
      (is (re-find #"jsonb#>'\{user,name\}'" sql)))))

(deftest json-path-text-operator-test
  (testing "generates #>> JSONB path text accessor SQL"
    (let [query {:select [(sql/call :#>> :data [:user :name])]}
          [sql] (sql/format query)]
      (is (re-find #"jsonb#>>'\{user,name\}'" sql)))))

;; -- JSONB helper function tests

(deftest json>-test
  (testing "single key accessor"
    (let [result (core/json> :data :name)
          [sql] (sql/format {:select [result]})]
      (is (re-find #"jsonb->'name'" sql))))

  (testing "path accessor"
    (let [result (core/json> :data [:user :name])
          [sql] (sql/format {:select [result]})]
      (is (re-find #"jsonb#>'\{user,name\}'" sql))))

  (testing "with type cast"
    (let [result (core/json> :data :age :integer)
          [sql] (sql/format {:select [result]})]
      (is (re-find #"::integer" sql)))))

(deftest json>>-test
  (testing "single key text accessor"
    (let [result (core/json>> :data :name)
          [sql] (sql/format {:select [result]})]
      (is (re-find #"jsonb->>'name'" sql))))

  (testing "path text accessor"
    (let [result (core/json>> :data [:user :name])
          [sql] (sql/format {:select [result]})]
      (is (re-find #"jsonb#>>'\{user,name\}'" sql))))

  (testing "with type cast"
    (let [result (core/json>> :data :count :integer)
          [sql] (sql/format {:select [result]})]
      (is (re-find #"::integer" sql)))))

;; -- Other helper tests

(deftest total-test
  (testing "total is raw SQL window function"
    (let [query {:select [:id :name core/total] :from [:users]}
          [sql] (sql/format query)]
      (is (re-find #"COUNT\(\*\) OVER \(\)" sql)))))

(deftest json-agg-test
  (testing "creates json_agg subquery structure"
    (let [inner {:select [:id :name] :from [:users]}
          result (core/json-agg inner)]
      (is (map? result))
      (is (contains? result :select))
      (is (contains? result :from))
      (is (= :x (second (first (:from result))))))))

;; -- ConnectionPool tests

(deftest connection-pool-record-test
  (testing "ConnectionPool implements Closeable"
    (let [mock-ds (reify Closeable (close [_] nil))
          pool (core/->ConnectionPool mock-ds)]
      (is (instance? Closeable pool))))

  (testing "ConnectionPool acts as a map with :datasource key"
    (let [mock-ds (reify Closeable (close [_] nil))
          pool (core/->ConnectionPool mock-ds)]
      (is (= mock-ds (:datasource pool)))
      (is (contains? pool :datasource))))

  (testing "close-pool calls .close on the pool"
    (let [closed? (atom false)
          mock-ds (reify Closeable (close [_] (reset! closed? true)))
          pool (core/->ConnectionPool mock-ds)]
      (core/close-pool pool)
      (is @closed?)))

  (testing "pool works with with-open"
    (let [closed? (atom false)
          mock-ds (reify Closeable (close [_] (reset! closed? true)))
          pool (core/->ConnectionPool mock-ds)]
      (with-open [p pool]
        (is (= mock-ds (:datasource p))))
      (is @closed?))))
