# Workflo Macros

A collection of Clojure and ClojureScript macros for web and mobile
development.

```clojure
[workflo/macros "0.2.5"]
```

[CHANGELOG](CHANGELOG.md)

## `defview` - Define Om Next components in a compact way

Om Next views are a combination of an Om Next component defined
with `om.next/defui` and a component factory created with
`om.next/factory`. The `defview` macro combines these two into
a single definition and reduces the boilerplate code needed to
define properties, idents, React keys, queries and component
functions.

### Defining views

Views are defined using `defview`, accepting the following
information:

* A destructuring form for properties (optional)
* A destructuring form for computed properties (optional)
* A `key` or `keyfn` function (optional)
* A `validate` or `validator` function (optional)
* An arbitrary number of regular Om Next, React or JS
  functions (e.g. `query`, `ident`, `componentWillMount`
  or `render`).

A `(defview UserProfile ...)` expression defines both a
`UserProfile` component and a `user-profile` component
factory.

### Properties destructuring

All properties declared via the destructuring forms are
made available in component functions via an implicit
`let` statement wrapping the original function body in
the view definition.

As an example, the properties destructuring form

```clojure
[user [name email {friends ...}] [current-user _]]
```

would destructure the properties as follows:

```clojure
:user/name    -> name
:user/email   -> email
:user/friends -> friends
:current-user -> current-user
```

### Automatic Om Next query generation

The `defview` macro predefines `(query ...)` for any view based
on the properties destructuring form. Auto-generation currently
supports joins and links but not unions and parameterization.

As an example, the properties destructuring form

```clojure
[user [name email {friends User}] [current-user _]]
```

would generate the following `query` function:

```clojure
static om.next/IQuery
(query [this]
  [:user/name
   :user/email
   {:user/friends (om/get-query User)}
   [:current-user _]])
```

This can be overriden simply by implementing your own `query`:

```clojure
(defview User
  [...]
  (query
    [:name :email]))
```

In the future, we will likely add a simple way to transform
auto-generated queries. On idea is to implicitly bind the
auto-generated query when overriding `query` and providing
convenient methods to parameterize sub-queries, e.g.

```clojure
(query
  (-> auto-query
      (set-param :user/friends :param :value)
      (set-param :current-user :id [:user 15])))
```

### Automatic inference of `ident` and `:keyfn`

If the properties spec includes `[db [id]]`, corresponding to
the Om Next query attribute `:db/id`, it is assumed that the
view represents data from DataScript or Datomic. In this case,
`defview` will automatically infer `(ident ...)` and
`(key ...)` / `:keyfn` functions based on the database ID. This
behavior can be overriden by specifically defining both ident
and key."

As an example:

```
(defview User
  [db [id] user [name]])
```

will result in the equivalent of:

```
(defui User
  static om/Ident
  (ident [this {:keys [db/id]}]
    [:db/id id]))

(def user (om/factory User {:keyfn :db/id}))
```

### Implicit binding of `this` and `props` in functions

The names `this` and `props` are available inside
function bodies depending on their signature (e.g. `render`
only makes `this` available, whereas `ident` pre-binds `this`
and `props`).

### Custom (raw) functions with their own argument vectors

By default, `defview` implicitly assumes the arguments to custom
functions, i.e., functions other than Om Next and React lifecycle
functions, are to be `[this]`. However, when adding JS object functions
to a view, you'll often want additional arguments. Here is an example
highlighting the problem:

```clojure
(defview UserList
  [users]
  (select ;; implictly adds [this]
    ;; where should the user ID come from?
    (om/transact! this `[(users/select {:user ~???})])))
```

To solve this problem, `defview` supports a `.` syntax for function
definitions. Any function that starts with a `.` will become a JS
object function and its arguments will remain untouched:

```clojure
(defview UserList
  [users]
  (.select [this user]
    (om/transact! this `[(users/select {:user ~user})])))
```

Inside the view, this function can now be called with
`(.select this <user id>)`, just like any other JS object function.

### Example

```clojure
(ns foo.bar
  (:require [workflo.macros.view :refer-macros [defview]]))

(defview User
  [user [name email address {friends ...}]]
  [ui [selected?] select-fn]
  (key name)
  (validate (string? name))
  (ident [:user/by-name name])
  (render
    (dom/div #js {:className (when selected? "selected")
                  :onClick (select-fn (om/get-ident this))}
      (dom/h1 nil "User: " name)
      (dom/p nil "Address: " street " " house))))
```

Example usage in Om Next:

```clojure
(user (om/computed {:user/name "Jeff"
                    :user/email "jeff@jeff.org"
                    :user/address {:street "Elmstreet"
                                   :house 13}}
                   {:ui/selected? true
                    :select-fn #(js/alert "Selected!")))
```

### `defview` internals

Most of the internals of the `defview` macro are available as
separate functions for easy reuse:

```clojure
(require '[workflo.macros.props.util :as u]
         '[workflo.macros.props :as p]
         '[workflo.macros.view :as v])

;; Utilities

(u/combine-properties-and-groups '[foo [bar [baz ruux]]])
-> [foo bar [baz ruux]]

(u/capitalized-name 'user)  -> User
(u/capitalized-name 'User)  -> User
(u/capitalized-name :user)  -> User
(u/capitalized-name "user") -> User

(u/capitalized-symbol? 'user)     -> false
(u/capitalized-symbol? 'userName) -> false
(u/capitalized-symbol? '_)        -> false
(u/capitalized-symbol? 'User)     -> true
(u/capitalized-symbol? 'UserName) -> true

;; Parser for properties destructuring forms

(p/parse '[user [name email {friend ...}] [current-user _]])
-> [{:name user/name :type :property}
    {:name user/email :type :property}
    {:name user/friend :type :join :join-target ...}
    {:name current-user :type :link :link-id _}]

;; Property queries based on parsed property forms

(p/property-query '{:name foo :type :property})
-> :foo

(p/property-query '{:name foo/bar :type :join :join-target Foo})
-> {:foo/bar (om/get-query Foo)}

(p/property-query '{:name bar :type :join :join-target ...})
-> {:bar ...}

(p/property-query '{:name bar :type :join :join-target 5})
-> {:bar 5}

(p/property-query '[current-user _])
-> [:current-user _]

;; Om Next query generation

(p/om-query '[{:name user/name :type :property}
              {:name user/email :type :property}
              {:name user/friend :type :join :join-target ...}
              {:name current-user :type :link :link-id _}])
-> [:user/name
    :user/email
    {:user/friend ...}
    [:current-user _]]
```

## License

Workflo Macros is copyright (C) 2016 Workflo. Licensed under the
MIT License. For more information [see the LICENSE file](LICENSE).
