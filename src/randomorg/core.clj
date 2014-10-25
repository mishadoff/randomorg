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
   replacement - true may return duplicates, false return all unique numbers, default is true
   base - base for numbers, default is 10
"
  [& {:keys [n min max replacement base]
      :as request-data
      :or {replacement true base 10}}]
  ;; TODO validate
  (-> (make-request)
      (assoc :method "generateIntegers")
      (assoc :params request-data)
      (assoc-in [:params :apiKey] API_KEY)
      (assoc :id 0) ;; simple stub as we don't really care about it
      (json/write-str :key-fn name)
      (post-json)
      (as-> json-result
            (cond-> json-result
                    ;; succesful request
                    (= 200 (:status json-result))
                    (-> (:body json-result)
                        (json/read-str :key-fn keyword)
                        (get-in [:result :random :data]))
                    
                    (= 500 (:status json-result))
                    :error
                    ))))
