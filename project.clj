(defproject randomorg "0.1.0"
  :description "Random generator via atmospheric noise random.org"
  :url "https://github.com/mishadoff/randomorg"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.5"]
                 [clj-http "1.0.1"]
                 [bouncer "0.3.1"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.0.0"]
                             ;; static code analyzers
                             [jonase/eastwood "0.1.4"]
                             [lein-kibit "0.0.8"]
                             [lein-bikeshed "0.1.8"]]}})
