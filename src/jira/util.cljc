(ns jira.util
  (:require
    clojure.data
    [cognitect.transit :as t])
  #?(:clj
     (:import
      (java.io ByteArrayInputStream ByteArrayOutputStream))))



(defn transit->obj [s]
  #?(:clj
     (-> s
       (.getBytes "UTF-8")
       (ByteArrayInputStream.)
       (t/reader :json)
       (t/read))
     :cljs
     (t/read (t/reader :json) s)))


(defn obj->transit [o]
  #?(:clj
     (let [os (ByteArrayOutputStream.)]
       (t/write (t/writer os :json) o)
       (String. (.toByteArray os) "UTF-8"))
     :cljs
     (t/write (t/writer :json ) o)))


(defn any? [coll]
  (not (not-any? true? coll)))


(defn- editable? [coll]
  #?(:clj  (instance? clojure.lang.IEditableCollection coll)
     :cljs (satisfies? cljs.core.IEditableCollection coll)))

(defn- reduce-map [f coll]
  (if (editable? coll)
    (persistent! (reduce-kv (f assoc!) (transient (empty coll)) coll))
    (reduce-kv (f assoc) (empty coll) coll)))

(defn filter-vals
  "Returns a new associative collection of the items in coll for which
  `(pred (val item))` returns true."
  [pred coll]
  (reduce-map (fn [xf] (fn [m k v] (if (pred v) (xf m k v) m))) coll))
