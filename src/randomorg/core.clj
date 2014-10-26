(ns randomorg.core
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [randomorg.validator :as v]))

(def API_ENDPOINT "https://api.random.org/json-rpc/1/invoke")

;; TODO remove
(def API_KEY "3ede1e75-e3d9-4298-89a3-2ef49e5f1143")

(defn- make-request []
  {:jsonrpc "2.0"
   :method nil
   :params nil
   :id nil})

(defn- post-json [json]
  (http/post API_ENDPOINT
             {:content-type :json
              :body json}))

(defn- make-success [data]
  {:status :success
   :data data})

(defn- make-error [message]
  {:status :error
   :data message})

(defn- request-processor [method data]
  (-> (make-request)
      (assoc :method method)
      (assoc :params data)
      (assoc-in [:params :apiKey] API_KEY)
      (assoc :id 0) ;; simple stub as we don't really care about it
      (json/write-str :key-fn name)
      (post-json)
      ((fn [json-result]
         (print json-result)
         (case (:status json-result)
           ;; Bug: 200 could return error
           200 (-> (:body json-result)
                   (json/read-str :key-fn keyword)
                   (get-in [:result :random :data])
                   make-success)
           
           500 (-> (:body json-result)
                   (json/read-str :key-fn keyword)
                   (get-in [:error :message])
                   make-error)
           
           ;; not handled
           (throw (IllegalArgumentException.
                   (format "Not handled status %s" (:status json-result))))
           )))))

(defn generate-integers
  "Generates true random integers within user-defined range.

   Required Parameters:
   n - number of integers, [1, 1e4]
   min - lower boundary for the range, [-1e9, 1e9]
   max - upper boundary for the range, [-1e9, 1e9]

   Optional Parameters:
   replacement - true may return duplicates, false return all unique numbers, default is true
   base - base for numbers, default is 10
"
  [& {:keys [n min max replacement base]
      :as raw-request-data}]
  (let [request-data (-> (merge {:replacement true :base 10} raw-request-data)
                         (select-keys [:n :min :max :replacement :base]))]
    (v/validate request-data
                :n v/n-validator
                :min v/range-1e9-validator
                :max v/range-1e9-validator
                :replacement v/boolean
                :base v/base-validator)
    
    (request-processor "generateIntegers" request-data)))


(defn generate-decimal-fractions
  "Generates decimal fractions from a uniform distribution across the [0, 1] interval

   Required Parameters:
   n - number of decimals, [1, 1e4]
   decimalPlaces - number of decimal places, [1, 20]

   Optional Parameters:
   replacement - true may return duplicates, false return all unique numbers, default is true
"
  [& {:keys [n decimalPlaces replacement]
      :as raw-request-data}]
  (let [request-data (-> (merge {:replacement true} raw-request-data)
                         (select-keys [:n :decimalPlaces :replacement]))]
    (v/validate request-data
                :n v/n-validator
                :decimalPlaces v/decimal-range-validator
                :replacement v/boolean)
    
    (request-processor "generateDecimalFractions" request-data)))

(defn generate-gaussians
  "Generates decimal fractions from a normal distribution

   Required Parameters:
   n - number of decimals, [1, 1e4]
   mean - mean of distribution, [-1e6, 1e6]
   standardDeviation - standard deviation, [-1e6, 1e6]
   significantDigits - significant digits, [2, 20]

   Optional Parameters:
   None
"
  [& {:keys [n mean standardDeviation significantDigits]
      :as raw-request-data}]
  (let [request-data (select-keys raw-request-data [:n :mean :standardDeviation :significantDigits])]
    (v/validate request-data
              :n v/n-validator
              :mean v/range-1e6-validator
              :standardDeviation v/range-1e6-validator
              :significantDigits v/significant-digits-validator)
    
    (request-processor "generateGaussians" request-data)))

(defn generate-strings
  "Generates random strings from specified characters

   Required Parameters:
   n - number of strings, [1, 1e4]
   length - the length of each string, [1, 20]
   characters - string that contains set of characters, [1, 80]

   Optional Parameters:
   replacement - true may return duplicates, false return all unique strings, default is true
"
  [& {:keys [n length characters replacement]
      :as raw-request-data}]
  (let [request-data (-> (merge {:replacement true} raw-request-data)
                         (select-keys [:n :length :characters :replacement]))]
    (v/validate request-data
              :n v/n-validator
              :length v/string-length-validator
              :characters v/characters-validator
              :replacement v/boolean)
    
    (request-processor "generateStrings" request-data)))

(defn generate-uuids
  "Generates random uuids

   Required Parameters:
   n - number of uuids, [1, 1e3]

   Optional Parameters:
   None
"
  [& {:keys [n]
      :as raw-request-data}]
  (let [request-data (select-keys raw-request-data [:n])]
    (v/validate request-data
                :n v/n-uuid-validator)
    
    (request-processor "generateUUIDs" request-data)))
