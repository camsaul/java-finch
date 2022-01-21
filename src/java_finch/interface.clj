(ns java-finch.interface)

(def source-hierarchy
  (atom (make-hierarchy)))

;; TODO -- some way to pass additional options e.g. to allow listing beta versions

(defmulti list-remote
  "List the available remote JVM versions for `source`."
  {:arglists '([source architecture os])}
  (fn [source _architecture _os]
    (keyword source))
  :hierarchy source-hierarchy)

(defmulti list-installed
  "List locally-installed JVM versions for `source`."
  {:arglists '([source])}
  keyword
  :hierarchy source-hierarchy)

(defmulti use!
  "Use `version`."
  {:arglists '([source architecture os version])}
  (fn [source _architecture _os _version]
    (keyword source))
  :hierarchy source-hierarchy)
