(ns randomorg.core
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]))


(def API_ENDPOINT "https://api.random.org/json-rpc/1/invoke")

;; TODO remove
(def API_KEY "3ede1e75-e3d9-4298-89a3-2ef49e5f1143")


(defn- make-request []
  {:jsonrpc "2.0"
   :method nil
   :params nil
   :id nil})

(defn post-json [json]
  (http/post API_ENDPOINT
             {:content-type :json
              :body json}))

(defn generate-integers
  "Generates true random integers within user-defined range.\n

   Required Parameters:
   n - number of integers, [1, 1e4]
   min - lower boundary for the range, [-1e9, 1e9]
   max - upper boundary for the range, [-1e9, 1e9]

   Optional Parameters:
   replacement -
   base -
"
  [n min max]
  ;; TODO validate
  (-> (make-request)
      (assoc :method "generateIntegers")
      (assoc :params {:n n
                      :min min
                      :max max
                      :apiKey API_KEY})
      (assoc :id 0) ;; simple stub as we don't really care about it
      (json/write-str :key-fn name)
      (post-json)
      :body
      (json/read-str :key-fn keyword)
      :result
      :random
      :data
      ))
