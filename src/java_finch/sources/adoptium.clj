(ns java-finch.sources.adoptium
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.string :as str]
            [java-finch.interface :as i]
            [java-finch.remote-source :as remote]))

(swap! i/source-hierarchy derive :adoptium :remote-source)

(defn- ->url [url & {:as query-args}]
  (str url (when (seq query-args)
             (str "?" (str/join "&" (for [[k v] query-args]
                                      (format "%s=%s" (name k) (str v))))))))

(defn- get-json [url & query-args]
  ;; TODO -- send request headers
  (let [url                   (apply ->url url query-args)
        {:keys [status body]} (curl/get url)]
    (when-not (= status 200)
      (throw (ex-info (format "Expected status code 200, got %d" status)
                      {:body body})))
    (json/parse-string body keyword)))

(defmethod i/list-remote :adoptium
  [_ architecture os]
  (->> (get-json "https://api.adoptium.net/v3/info/release_versions"
                 :page_size    50
                 :image_type   "jdk"
                 :release_type "ga"
                 :os           (name os)
                 :architecture (name architecture))
       :versions
       (map :semver)))

(defmethod remote/remote-url :adoptium
  [source architecture os version]
  (let [{[{{:keys [checksum link]} :package}] :binaries} (get-json (format "https://api.adoptium.net/v3/assets/version/%s" version)
                                                                   :release_type "ga"
                                                                   :image_type   "jdk"
                                                                   :architecture architecture
                                                                   :os           os)]
    {:checksum checksum, :url link}))
