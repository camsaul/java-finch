(ns java-finch.core
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [java-finch.interface :as i]
            ;; TODO -- we should load everything in the sources dir automatically.
            java-finch.sources.adoptium))

;; TODO -- determine this automatically
;; TODO -- allow override.
(def ^:dynamic *architecture* "x64")
(def ^:dynamic *os* "linux")

(defmulti command
  {:arglists '([command & args])}
  (fn
    ([]
     :help)
    ([command & _]
     (keyword command)))
  :default :help)

(defmethod command :help
  [& _]
  (println "Available commands:")
  (doseq [command (sort (keys (methods command)))]
    (println (name command)))) ; TODO -- print better help

(defn- sources []
  (set (keys (methods i/list-remote))))

(defmethod command :list-remote
  ([_]
   (println "Available versions:")
   (doseq [source  (sort (sources))
           version (i/list-remote source *architecture* *os*)]
     (println (pr-str source) (pr-str version))))

  ([_ source]
   (println "Available versions:")
   (doseq [version (i/list-remote (keyword source) *architecture* *os*)]
     (println (pr-str source) (pr-str version)))))

(defmethod command :list-installed
  [_]
  (println "Installed")
  nil)

(defmethod command :list
  [_]
  (command :list-installed)
  (println "\n")
  (command :list-remote))

(defmethod command :use
  [_ source version]
  (println (pr-str (list `i/use source *architecture* *os* version)))
  (i/use! source *architecture* *os* version))

(defn -main
  ([]
   (command :help))

  ([command-name & args]
   (let [command-name (keyword command-name)
         args         (for [arg args]
                        (if (and (string? arg)
                                 (str/starts-with? arg ":"))
                          (keyword (str/join (rest arg)))
                          arg))]
     (println (pr-str (list* `command command-name args)))
     (apply command command-name args))))
