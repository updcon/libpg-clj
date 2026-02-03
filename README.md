# libpg-clj

DKD commons for PostgreSQL written in Clojure. The matter is to make PG interactions more frictionless.

## Installation

### Leiningen/Boot

```clojure
[ai.z7/libpg-clj "0.1.0"]
```

### deps.edn

```clojure
ai.z7/libpg-clj {:mvn/version "0.1.0"}
```

## Quick Start

### Connection Pool Setup

```clojure
(require '[libpg-clj.core :as pg])

(def pool (pg/make-pool {:classname   "org.postgresql.Driver"
                         :subprotocol "postgresql"
                         :subname     "//localhost:5432/mydb"
                         :user        "postgres"
                         :password    "secret"
                         :min-pool    2
                         :max-pool    10}))

;; Use with clojure.java.jdbc
(require '[clojure.java.jdbc :as jdbc])
(jdbc/query pool ["SELECT * FROM users"])

;; Close when done
(pg/close-pool pool)
```

### Using with-open (Recommended)

The connection pool implements `Closeable`, so you can use it with `with-open` for automatic resource management:

```clojure
(with-open [pool (pg/make-pool config)]
  (jdbc/query pool ["SELECT * FROM users"]))
;; Pool is automatically closed when exiting the block
```

## API Reference

### Connection Pool

#### `make-pool`

Creates a c3p0 connection pool for PostgreSQL.

```clojure
(make-pool {:classname         "org.postgresql.Driver"
            :subprotocol       "postgresql"
            :subname           "//localhost:5432/mydb"
            :user              "postgres"
            :password          "secret"
            :min-pool          2
            :max-pool          10
            :prepare-threshold 0})  ; optional, default 0
```

Returns a `ConnectionPool` record (implements `Closeable`) with `:datasource` key, suitable for use with `clojure.java.jdbc` and `with-open`.

#### `close-pool`

Closes a connection pool, releasing all resources.

```clojure
(close-pool pool)

;; Equivalent to:
(.close pool)

;; Or use with-open for automatic cleanup
```

### Data Helpers

#### `convert-enum`

Converts a field value to a `[value type]` pair for enum casting in queries.

```clojure
(def type-map {:status "user_status"})
(convert-enum :status type-map {:status "active" :name "John"})
;=> {:status ["active" "user_status"] :name "John"}
```

#### `add-time-label`

Adds a UUID v1 timestamp to a map (useful for time-ordered identifiers).

```clojure
(add-time-label {:name "test"} :created-at)
;=> {:name "test" :created-at #uuid "..."}
```

#### `drop-fields`

Removes specified fields from a map.

```clojure
(drop-fields {:a 1 :b 2 :c 3} :a :c)
;=> {:b 2}
```

### Query Execution

#### `jdbc-exec`

Executes a query and returns the first result (typically row count).

```clojure
(jdbc-exec pool ["UPDATE users SET active = true WHERE id = ?" 123])
;=> 1
```

### Enum Utilities

#### `explain-enum`

Retrieves all possible values for a PostgreSQL enum type.

```clojure
(explain-enum pool "user_status")
;=> ["active" "inactive" "pending"]
```

#### `kw->pgenum`

Converts a namespaced keyword to a PostgreSQL enum. The namespace becomes the type (with `-` replaced by `_`), and the name becomes the value.

```clojure
(kw->pgenum :user-status/active)
;=> PGobject with type="user_status" value="active"
```

This conversion happens automatically when using namespaced keywords as values in queries.

### HoneySQL Helpers

#### `h-cast`

Creates a PostgreSQL cast expression for HoneySQL.

```clojure
(h-cast :my-field :integer)
;=> Generates: my_field::integer
```

#### `total`

A raw SQL window function for counting total rows (useful for pagination).

```clojure
{:select [:id :name total]
 :from   [:users]
 :limit  10}
;=> SELECT id, name, COUNT(*) OVER () FROM users LIMIT 10
```

#### `json-agg`

Wraps a subquery with JSON aggregation.

```clojure
(json-agg {:select [:id :name] :from [:users]})
;=> SELECT json_agg(x) FROM (SELECT id, name FROM users) x
```

## HoneySQL Operators

This library extends HoneySQL with PostgreSQL-specific operators:

| Operator | SQL | Example |
|----------|-----|---------|
| `:ilike` | `ILIKE` | `(call :ilike :name "test%")` => `name ILIKE 'test%'` |
| `:contains` | `@>` | `(call :contains :tags val)` => `tags @> val` |
| `:array-exists` | `??` | `(call :array-exists :data "key")` => `data ?? 'key'` |
| `:array-exists-any` | `??|` | `(call :array-exists-any :data arr)` => `data ??| arr` |
| `:cast` | `::` | `(call :cast :val :integer)` => `val::integer` |
| `:->` | `->` | `(call :-> :data :key)` => `(data::jsonb->'key')` |
| `:->>` | `->>` | `(call :->> :data :key)` => `(data::jsonb->>'key')` |
| `:#>` | `#>` | `(call :#> :data [:a :b])` => `(data::jsonb#>'{a,b}')` |
| `:#>>` | `#>>` | `(call :#>> :data [:a :b])` => `(data::jsonb#>>'{a,b}')` |

## JSONB Helpers

Two convenience functions for JSONB field access:

### `json>` - Returns JSON (preserves type)

```clojure
;; Single key
(json> :data :name)           ; data::jsonb->'name'

;; Path access
(json> :data [:user :name])   ; data::jsonb#>'{user,name}'

;; With cast
(json> :data :age :integer)   ; (data::jsonb->'age')::integer
```

### `json>>` - Returns text (always string)

```clojure
;; Single key
(json>> :data :name)           ; data::jsonb->>'name'

;; Path access
(json>> :data [:user :name])   ; data::jsonb#>>'{user,name}'

;; With cast
(json>> :data :count :integer) ; (data::jsonb->>'count')::integer
```

## Type Conversions

### Automatic Keyword to Enum

Namespaced keywords are automatically converted to PostgreSQL enums:

```clojure
;; This query:
{:insert-into :users
 :values [{:name "John" :status :user-status/active}]}

;; Automatically converts :user-status/active to a PGobject
;; with type "user_status" and value "active"
```

### Automatic JSONB Reading

JSONB and JSON columns are automatically parsed into Clojure data structures:

```clojure
;; If users.metadata is JSONB containing {"role": "admin"}
(jdbc/query pool ["SELECT metadata FROM users WHERE id = ?" 1])
;=> [{:metadata {:role "admin"}}]
```

## Debug Utilities

The `libpg-clj.debug` namespace provides query debugging tools:

### `print-statement`

Prints a prepared statement with bound parameters.

```clojure
(require '[libpg-clj.debug :as debug])

(debug/print-statement pool ["SELECT * FROM users WHERE id = ?" 123])
;; Prints the PreparedStatement object
```

### `query-explain`

Runs EXPLAIN ANALYZE on a query and pretty-prints the execution plan.

```clojure
(debug/query-explain pool ["SELECT * FROM users WHERE id = ?" 123])
;; Prints the query execution plan
```

## License

Distributed under the MIT License.
