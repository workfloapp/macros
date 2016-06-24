(ns workflo.macros.specs.conforming-query
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.util :as util]
            [workflo.macros.specs.query]))

;;;; Simple properties

(s/def ::simple-property
  (s/tuple #{:simple}
           :workflo.macros.specs.query/property-name))

;;;; Links

(s/def ::link
  (s/tuple #{:link}
           :workflo.macros.specs.query/link))

;;;; Joins

(s/def ::model-join
  (s/tuple #{:model-join}
           :workflo.macros.specs.query/model-join))

(s/def ::recursive-join
  (s/tuple #{:recursive-join}
           :workflo.macros.specs.query/recursive-join))

(s/def ::properties-join
  (s/tuple #{:properties-join}
           :workflo.macros.specs.query/properties-join))

(s/def ::join
  (s/tuple #{:join}
           (s/or :model-join ::model-join
                 :recursive-join ::recursive-join
                 :properties-join ::properties-join)))

;;;; Individual properties

(s/def ::property-value
  (s/or :simple ::simple-property
        :link   ::link
        :join   ::join))

(s/def ::property
  (s/tuple #{:property} ::property-value))

;;;; Nested properties

(s/def ::base
  :workflo.macros.specs.query/property-name)

(s/def ::children
  (s/with-gen
    (s/and vector? (s/+ ::property-value))
    #(gen/vector (s/gen ::property-value)
                 1 10)))

(s/def ::nested-properties-value
  (s/keys :req-un [::base ::children]))

(s/def ::nested-properties
  (s/tuple #{:nested-properties} ::nested-properties-value))

;;;; Queries

(s/def ::regular-query-value
  (s/or :property ::property
        :nested-properties ::nested-properties))

(s/def ::regular-query
  (s/tuple #{:regular-query} ::regular-query-value))

(s/def ::parameters
  :workflo.macros.specs.query/parameters)

(s/def ::parameterized-query-value
  (s/keys :req-un [::regular-query-value ::parameters]))

(s/def ::parameterized-query
  (s/tuple #{:parameterized-query} ::parameterized-query-value))

(s/def ::query
  (s/with-gen
    (s/and vector?
           (s/+ (s/alt :regular-query ::regular-query
                       :parameterized-query ::parameterized-query)))
    #(gen/vector (gen/one-of [(s/gen ::regular-query)
                              (s/gen ::parameterized-query)])
                 1 10)))
