(ns randomorg.core
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [randomorg.validator :as v]))

(def API_ENDPOINT "https://api.random.org/json-rpc/1/invoke")

;; some api key for testing/playground purposes
;; request your own api key 
(def ^:dynamic *API_KEY*
  "3ede1e75-e3d9-4298-89a3-2ef49e5f1143")

(defn- make-request []
  {:jsonrpc "2.0"
   :method nil
   :params nil
   :id nil})

(defn- post-json [json]
  (http/post API_ENDPOINT
             {:content-type :json
              :body json}))

(defn- make-success [data usage]
  {:status :success
   :data data
   :usage usage})

(defn- make-error [message]
  {:status :error
   :data message})

(defn api-usage-processor [json-response]
  (select-keys json-response [:bitsLeft :requestsLeft :totalBits :totalRequests]))

(defn signed-data-processor [json-response]
  {:hashed-api-key
   (get-in json-response [:result :random :hashedApiKey])
   :signature
   (get-in json-response [:result :signature])})

(defn- request-processor [method data]
  (let [signed (get data :signed false)]
    (-> (make-request)
        (assoc :method method)
        (assoc :params (dissoc data :signed))
        (assoc-in [:params :apiKey] *API_KEY*)
        (assoc :id 0) ;; simple stub as we don't really care about it
        (json/write-str :key-fn name)
        (post-json)
        ((fn [response]
           (print response) ;; TODO remove
           (case (:status response)
             200 (let [json (-> (:body response)
                                (json/read-str :key-fn keyword))
                       result (:result json)
                       data (get-in result [:random :data])
                       usage (api-usage-processor result)
                       error (get-in json [:error :message])
                       signed-data (signed-data-processor json)]
                   (cond
                    result (-> (make-success data usage)
                               ((fn [success]
                                  (if signed
                                    (assoc success :signed signed-data)
                                    success))))
                    error (make-error error)
                    :else (make-error nil usage)))
             
             ;; not handled
             (make-error (:status response))
             ))))))

(defn- generate-integers
  "Generates true random integers within user-defined range.

   Required Parameters:
   n - number of integers, [1, 1e4]
   min - lower boundary for the range, [-1e9, 1e9]
   max - upper boundary for the range, [-1e9, 1e9]

   Optional Parameters:
   replacement - true may return duplicates, false return all unique numbers, default is true
   base - base for numbers, default is 10
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG
"
  [& {:keys [n min max replacement base signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:replacement true :base 10 :signed false} raw-request-data)
                         (select-keys [:n :min :max :replacement :base :signed]))]
    (v/validate request-data
                :n v/n-validator
                :min v/range-1e9-validator
                :max v/range-1e9-validator
                :replacement v/boolean
                :base v/base-validator
                :signed v/boolean)
    (request-processor
     (if signed "generateSignedIntegers" "generateIntegers")
     request-data)))

(defn generate-decimal-fractions
  "Generates decimal fractions from a uniform distribution across the [0, 1] interval

   Required Parameters:
   n - number of decimals, [1, 1e4]
   decimalPlaces - number of decimal places, [1, 20]

   Optional Parameters:
   replacement - true may return duplicates, false return all unique numbers, default is true
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG
"
  [& {:keys [n decimalPlaces replacement signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:replacement true :signed false} raw-request-data)
                         (select-keys [:n :decimalPlaces :replacement :signed]))]
    (v/validate request-data
                :n v/n-validator
                :decimalPlaces v/decimal-range-validator
                :replacement v/boolean
                :signed v/boolean)
    
    (request-processor
     (if signed
       "generateDecimalFractions"
       "generateSignedDecimalFractions") request-data)))

(defn generate-gaussians
  "Generates decimal fractions from a normal distribution

   Required Parameters:
   n - number of decimals, [1, 1e4]
   mean - mean of distribution, [-1e6, 1e6]
   standardDeviation - standard deviation, [-1e6, 1e6]
   significantDigits - significant digits, [2, 20]

   Optional Parameters:
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG

"
  [& {:keys [n mean standardDeviation significantDigits signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:signed false} raw-request-data)
                         (select-keys [:n :mean :standardDeviation :significantDigits :signed]))]
    (v/validate request-data
              :n v/n-validator
              :mean v/range-1e6-validator
              :standardDeviation v/range-1e6-validator
              :significantDigits v/significant-digits-validator
              :signed v/boolean)
    (request-processor
     (if signed "generateSignedGaussians" "generateGaussians")
     request-data)))

(defn generate-strings
  "Generates random strings from specified characters

   Required Parameters:
   n - number of strings, [1, 1e4]
   length - the length of each string, [1, 20]
   characters - string that contains set of characters, [1, 80]

   Optional Parameters:
   replacement - true may return duplicates, false return all unique strings, default is true
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG
"
  [& {:keys [n length characters replacement signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:replacement true :signed false} raw-request-data)
                         (select-keys [:n :length :characters :replacement :signed]))]
    (v/validate request-data
              :n v/n-validator
              :length v/string-length-validator
              :characters v/characters-validator
              :replacement v/boolean
              :signed v/boolean)
    
    (request-processor
     (if signed "generateSignedStrings" "generateStrings")
     request-data)))

(defn generate-uuids
  "Generates random uuids

   Required Parameters:
   n - number of uuids, [1, 1e3]

   Optional Parameters:
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG
"
  [& {:keys [n signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:signed false} raw-request-data)
                         (select-keys [:n :signed]))]
    (v/validate request-data
                :n v/n-uuid-validator
                :signed v/boolean)
    
    (request-processor
     (if signed "generateSignedUUIDs" "generateUUIDs") request-data)))

(defn generate-blobs
  "Generates random blobs

   Required Parameters:
   n - number of random blobs, [1, 100]
   size - the size of each blob measured in bits, must be in [1, 1048576] and divisible by 8

   Optional Parameters:
   format - specifies the format in which blob will be returned, allowed values [base64, hex], default is base64
"
  [& {:keys [n size format signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:format "base64" :signed false} raw-request-data)
                         (select-keys [:n :size :format :signed]))]
  (v/validate request-data
                :n v/n-blob-validator
                :size v/blob-size-validator
                :format v/blob-format-validator
                :signed v/boolean)
    
    (request-processor
     (if signed "generateSignedBlobs" "generateBlobs")  request-data)))


(defn get-usage
  "Get API usage limits"
  []
  (request-processor "getUsage" {}))
