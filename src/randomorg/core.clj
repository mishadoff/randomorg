(ns randomorg.core
  (:require [clojure.data.json :as json]
            [clojure.set :as set]
            [clj-http.client :as http]
            [randomorg.validator :as v]))

(def API_ENDPOINT "https://api.random.org/json-rpc/1/invoke")

;; some api key for testing/playground purposes
;; request your own api key 
(def ^:dynamic *API_KEY*
  "3ede1e75-e3d9-4298-89a3-2ef49e5f1143")

;; character sets for generate-string
(def lowercase "abcdefghijklmnopqrstuvwxyz")
(def uppercase "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
(def digits "0123456789")

(defn- make-request []
  {:jsonrpc "2.0"
   :method nil
   :params nil
   :id nil})

(defn- post-json [json]
  (http/post API_ENDPOINT
             {:content-type :json
              :body json}))

(defn- make-success [data & {:keys [usage]}]
  (let [succ {:status :success
              :data data}]
    (if usage (assoc succ :usage usage) succ)))

(defn- make-error [message]
  {:status :error
   :message message})

(defn- api-usage-processor [json-response]
  (-> json-response
      (select-keys [:bitsLeft :requestsLeft :totalBits :totalRequests])
      (set/rename-keys {:bitsLeft :bits-left
                        :requestsLeft :requests-left
                        :totalBits :total-bits
                        :totalRequests :total-requests})))

(defn- signed-data-processor [json-response]
  {:signature (get-in json-response [:result :signature])
   :random (get-in json-response [:result :random])})

(defn- request-processor
  "Common method for processing request

   Required:
   name - remote API method,
   data - map with request parameters, will be encoded to json

   :signed property in data is not posted in request body, but used for getting signed structure of random


   Optional:
   :api-key boolean - some request don't need api-key, default is true
"
  [method data
   & {:keys [api-key] :or {api-key true}}]
  (let [signed (get data :signed false)]
    (-> (make-request)
        (assoc :method method)
        (assoc :params (dissoc data :signed))
        ((fn [req]
           (if api-key
             (assoc-in req [:params :apiKey] *API_KEY*)
             req)))
        (assoc :id 0) ;; simple stub as we don't really care about it
        (json/write-str :key-fn name)
        (post-json)
        ((fn [response]
           (case (:status response)
             200 (let [json (json/read-str (:body response) :key-fn keyword)
                       result (:result json)
                       data (get-in result [:random :data])
                       usage (api-usage-processor result)
                       error (get-in json [:error :message])
                       signed-data (signed-data-processor json)
                       verify (:authenticity result)]
                   (cond
                    verify (make-success verify)
                    result ((fn [success]
                              (if signed
                                (assoc success :signed signed-data)
                                success)) (make-success data :usage usage))
                    error (make-error error)
                    :else (make-error nil)))
             
             ;; not handled
             (make-error (:status response))
             ))))))

(defn generate-integers
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
  (let [request-data (select-keys
                      (merge {:replacement true :base 10 :signed false} raw-request-data)
                      [:n :min :max :replacement :base :signed])
        errors-map (v/validate request-data
                               :n v/n-validator
                               :min v/range-1e9-validator
                               :max v/range-1e9-validator
                               :replacement v/boolean
                               :base v/base-validator
                               :signed v/boolean)]
    (cond
     (empty? errors-map)
     (request-processor
      (if signed "generateSignedIntegers" "generateIntegers")
      request-data)
     :else (make-error errors-map))))

(defn generate-decimal-fractions
  "Generates decimal fractions from a uniform distribution across the [0, 1] interval

   Required Parameters:
   n - number of decimals, [1, 1e4]
   digits - number of decimal places, [1, 20]

   Optional Parameters:
   replacement - true may return duplicates, false return all unique numbers, default is true
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG
"
  [& {:keys [n digits replacement signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:replacement true :signed false} raw-request-data)
                         (select-keys [:n :digits :replacement :signed])
                         (set/rename-keys {:digits :decimalPlaces}))
        errors-map (v/validate request-data
                               :n v/n-validator
                               :decimalPlaces v/decimal-range-validator
                               :replacement v/boolean
                               :signed v/boolean)]
    
    (cond
     (empty? errors-map)
     (request-processor
      (if signed
        "generateSignedDecimalFractions"
        "generateDecimalFractions")
      request-data)
     :else (make-error errors-map))))

(defn generate-gaussians
  "Generates decimal fractions from a normal distribution

   Required Parameters:
   n - number of decimals, [1, 1e4]
   mean - mean of distribution, [-1e6, 1e6]
   std - standard deviation, [-1e6, 1e6]
   digits - significant digits, [2, 20]

   Optional Parameters:
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG

"
  [& {:keys [n mean std digits signed]
      :as raw-request-data}]
  (let [request-data (-> (merge {:signed false} raw-request-data)
                         (select-keys [:n :mean :std :digits :signed])
                         (set/rename-keys {:std :standardDeviation
                                           :digits :significantDigits}))]
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
  (let [request-data (select-keys
                      (merge {:replacement true :signed false} raw-request-data)
                      [:n :length :characters :replacement :signed])]
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
  (let [request-data (select-keys
                      (merge {:signed false} raw-request-data)
                      [:n :signed])]
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
   signed - methods produce digitally signed series of true random values can be proved to originate from RANDOM.ORG
"
  [& {:keys [n size format signed]
      :as raw-request-data}]
  (let [request-data (select-keys
                      (merge {:format "base64" :signed false} raw-request-data)
                      [:n :size :format :signed])]
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


(defn verify-signature
  "Check that random data realy comes from RANDOM.ORG"
  [{:keys [random signature] :as signed}]
  (v/validate signed
              :random v/required-random
              :signature v/required-signature)
  (request-processor "verifySignature" (select-keys signed [:random :signature]) :api-key false))
