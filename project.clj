(defproject randomorg "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.5"]
                 [clj-http "1.0.0"]
                 [bouncer "0.3.1"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [lein-midje "3.0.0"]]}})
