(ns java-finch.remote-source
  (:require [babashka.curl :as curl]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.pprint :as pprint]
            [java-finch.interface :as i]))

(defn install-directory [source architecture os version]
  ;; TODO -- don't hardcode this.
  (format "/home/cam/.java-finch/jvms/%s/%s/%s/%s"
          (name source)
          architecture
          os
          version))

(defmulti remote-url
  {:arglists '([source architecture os version])}
  (fn [source _architecture _os _version]
    (keyword source))
  :hierarchy i/source-hierarchy)

(defn- download! [{:keys [source architecture os version url dir]}]
  (println "DOWNLOAD =>" url)
  ;; TODO -- determine appropriate temp dir from env
  ;;
  ;; TODO -- don't assume file is `.tar.gz`
  (let [tmp-location (format "/tmp/%s.%s.%s.%s.tar.gz" (name source) architecture os version)]
    (println "TO =>" tmp-location)
    (if (fs/exists? tmp-location)
      (println tmp-location "already downloaded")
      ;; TODO -- we should print a nice progress bar while downloading m8
      (sh/sh "wget" "-O" tmp-location url)
      ;; TODO -- use `io/copy` so we're not dependent on having `wget` installed
      #_(io/copy
       (:body (curl/get url {:as :bytes}))
       (io/file tmp-location))
      ;; TODO -- verify file checksum if `checksum` was passed in
      )
    (fs/create-dirs dir)
    (println "EXTRACT TO =>" dir)
    (sh/sh "tar" "--extract"
           "-f" tmp-location
           "--directory" dir)))

(defn- install! [info]
  (pprint/pprint (list `install! info))
  (download! info))

(defmethod i/use! :remote-source
  [source architecture os version]
  ;; TODO -- check whether already installed
  (let [dir      (install-directory source architecture os version)
        url-info (remote-url source architecture os version)]
    (if (fs/exists? dir)
      (println dir "already exists")
      (install! (merge {:source       source
                        :architecture architecture
                        :os           os
                        :version      version
                        :dir          dir}
                       url-info)))
    ;; TODO -- don't hardcode this.
    (let [java-home (str dir "/" "jdk-17.0.1+12")]
      (println "Writing env vars to /home/cam/.java-finch/.env.sh")
      (spit "/home/cam/.java-finch/.env.sh"
            (str (format "export JAVA_HOME=\"%s\"\n" java-home)
                 (format "export PATH=\"%s:$PATH\"\n" (str java-home "/bin")))))))
