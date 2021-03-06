(ns workflo.macros.entity-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.entity :as e :refer [defentity]]))

(deftest minimal-defentity
  (is (= '(do
            (def macros-user-name 'macros/user)
            (def macros-user-spec map?)
            (def macros-user-definition
              {:name pod/macros-user-name
               :spec pod/macros-user-spec})
            (workflo.macros.entity/register-entity!
             'macros/user pod/macros-user-definition))
         (macroexpand-1 `(defentity macros/user
                           (~'spec ~'map?))))))

(deftest fully-qualified-entity-name
  (is (= '(do
            (def my-ui-app-name 'my.ui/app)
            (def my-ui-app-spec map?)
            (def my-ui-app-definition
              {:name pod/my-ui-app-name
               :spec pod/my-ui-app-spec})
            (workflo.macros.entity/register-entity!
             'my.ui/app pod/my-ui-app-definition))
         (macroexpand-1 `(defentity my.ui/app
                           (~'spec ~'map?))))))

(deftest defentity-with-hints
  (is (= '(do
            (def x-foo-name 'x/foo)
            (def x-foo-hints [:bar :baz])
            (def x-foo-spec map?)
            (def x-foo-definition
              {:name pod/x-foo-name
               :hints pod/x-foo-hints
               :spec pod/x-foo-spec})
            (workflo.macros.entity/register-entity!
             'x/foo pod/x-foo-definition))
         (macroexpand-1 `(defentity x/foo
                           (~'hints [:bar :baz])
                           (~'spec ~'map?))))))

(deftest defentity-with-auth
  (is (= '(do
            (def macros-user-name 'macros/user)
            (def macros-user-spec map?)
            (def macros-user-auth-query
              '[{:name foo :type :property}
                {:name bar :type :property}])
            (defn macros-user-auth
              [auth-query-result entity-id viewer-id]
              (workflo.macros.bind/with-query-bindings
                [{:name foo :type :property}
                 {:name bar :type :property}]
                auth-query-result
                (println foo bar)))
            (def macros-user-definition
              {:name pod/macros-user-name
               :spec pod/macros-user-spec
               :auth pod/macros-user-auth
               :auth-query pod/macros-user-auth-query})
            (workflo.macros.entity/register-entity!
             'macros/user pod/macros-user-definition))
         (macroexpand-1 `(defentity macros/user
                           ~'(spec map?)
                           ~'(auth-query [foo bar])
                           ~'(auth
                               (println foo bar)))))))
