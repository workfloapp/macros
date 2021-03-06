(ns workflo.macros.specs.types
  (:refer-clojure :exclude [bigdec? bytes? double? float? uri?])
  (:require [clojure.spec.alpha :as s :refer [Spec]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [workflo.macros.util.misc :refer [val-after]]))

;;;; Helpers

(defn long? [x]
  #?(:cljs (and (number? x)
                (not (js/isNaN x))
                (not (identical? x js/Infinity))
                (= 0 (rem x 1)))
     :clj  (instance? java.lang.Long x)))

(defn bigint? [x]
  #?(:cljs (long? x)
     :clj  (instance? clojure.lang.BigInt x)))

(defn float? [x]
  #?(:cljs (and (number? x)
                (not (js/isNaN x))
                (not (identical? x js/Infinity))
                (not= (js/parseFloat x) (js/parseInt x 10)))
     :clj  (clojure.core/float? x)))

(defn double? [x]
  #?(:cljs (float? x)
     :clj  (clojure.core/double? x)))

(defn bigdec? [x]
  #?(:cljs (float? x)
     :clj  (clojure.core/bigdec? x)))

(defn bytes? [x]
  #?(:cljs (array? x)
     :clj  (clojure.core/bytes? x)))

;;;; Fundamental types

(s/def ::any any?)
(s/def ::keyword keyword?)
(s/def ::string string?)
(s/def ::boolean boolean?)
(s/def ::long long?)
(s/def ::bigint bigint?)
(s/def ::float float?)
(s/def ::double double?)
(s/def ::bigdec bigdec?)
(s/def ::instant inst?)
(s/def ::uuid uuid?)
(s/def ::bytes bytes?)
(s/def ::enum keyword?)

;;;; Type options

(s/def ::unique-value any?)
(s/def ::unique-identity any?)
(s/def ::indexed any?)
(s/def ::fulltext any?)
(s/def ::component any?)
(s/def ::no-history any?)

;;;; Types whose values are not to be persisted

(s/def ::non-persistent any?)

;;;; Entity IDs

(s/def ::id
  (s/with-gen
    (s/and string? #(= (count %) 32))
    #(gen/fmap (fn [uuid]
                 (str/replace (str uuid) "-" ""))
               (gen/uuid))))

(s/def :workflo/id
  (s/and ::id ::unique-identity ::indexed))

;;;; Simple reference types

(s/def ::ref (s/keys :req [:workflo/id]))
(s/def ::ref-many (s/or :vector (s/coll-of ::ref :kind vector?)
                        :set (s/coll-of ::ref :kind set?)))

;;;; Entity references

(defn entity-ref-impl
  [entity-sym opts gfn]
  (let [subspec (s/spec (if (:many? opts) ::ref-many ::ref))]
    (reify Spec
      (conform* [_ x]
        (s/conform* subspec x))
      (unform* [_ x]
        (s/unform* subspec x))
      (explain* [_ path via in x]
        (s/explain* subspec path via in x))
      (gen* [_ overrides path rmap]
        (if gfn (gfn) (s/gen* subspec overrides path rmap)))
      (with-gen* [_ gfn]
        (entity-ref-impl entity-sym opts gfn))
      (describe* [_]
        `(entity-ref ~entity-sym ~@(mapcat identity opts))))))

(defn entity-ref
  [entity-sym & {:keys [many?] :or {many false} :as opts}]
  (entity-ref-impl entity-sym opts nil))

(defn entity-ref-opts
  [spec]
  (->> (s/describe spec)
       (drop-while (complement keyword?))
       (partition 2 2)
       (transduce (map vec) conj {})))

(defn entity-ref-info
  [spec]
  (merge {:entity (val-after (s/describe spec) 'entity-ref)}
         (entity-ref-opts spec)))

(defn entity-ref-from-description
  "Takes an `(entity-ref <name> & <args>)` description and returns
   an instance of the corresponding spec."
  [[_ entity-name & args]]
  (let [;; "Unquote" the entity name (if it is quoted)
        entity-name (cond-> entity-name
                      (and (seq? entity-name)
                           (= 'quote (first entity-name))
                           (symbol? (second entity-name)))
                      second)]
    (apply entity-ref (cons entity-name args))))
