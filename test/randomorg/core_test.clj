(ns randomorg.core-test
  (:require [midje.sweet :refer :all]
            [randomorg.core :refer :all]))

(def ^:dynamic *TESTING_API_KEY*
  "94c2fc83-d5bb-4396-856a-c7001a060d08")

;; api dumb-testing, we only check if results are returned
;; and not validate these results as they come from random.org

(defn error [result]
  (and (= (:status result) :error)
       (not (empty? (:message result)))))

(defn success [result]
  (and (= (:status result) :success)
       (not (empty? (:data result)))))

(defn signed [result]
  (and (not (empty? (get-in result [:signed :signature])))
       (not (empty? (get-in result [:signed :random])))))

(fact "generate-integers"
  (let [g generate-integers] ;; just an alias
    (fact "happy case"
      (g :n 1 :min 0 :max 10) => success
      (g :n 2 :min 0 :max 100) => success
      (g :n 10 :min -2 :max 2) => success)
    (fact "integration"
      (fact "two 6 sided dice rolls"
        (g :n 2 :min 1 :max 6) => success)
      (fact "shuffle 52 card deck"
        (g :n 52 :min 1 :max 52 :replacement false) => success))
    (fact "required params"
      (g) => error
      (g :n 1) => error
      (g :n 1 :min 0) => error
      (g :n 1 :max 0) => error
      (g :n 1 :replacement true :base 10) => error)
    (fact "n"
      (fact "integer"
        (g :n "1" :min 1 :max 10) => error
        (g :n 10.2 :min 1 :max 10) => error
      (fact "out of range"
        (g :n 0 :min 0 :max 10) => error
        (g :n 1e5 :min 0 :max 10) => error)))
    (fact "min"
      (fact "integer"
        (g :n 1 :min 0.1 :max 10) => error
        (g :n 1 :min "2" :max 10) => error
      (fact "out of range"
        (g :n 0 :min 1000000001 :max 1000000002) => error
        (g :n 1 :min -1000000001 :max 1) => error)))
    (fact "max"
      (fact "integer"
        (g :n 1 :min 0 :max 1.2) => error
        (g :n 1 :min 0 :max "1") => error
      (fact "out of range"
        (g :n 0 :min 1 :max 1000000001) => error
        (g :n 1 :min 0 :max -1000000001) => error)))
    (fact "replacement"
      (fact "boolean"
        (g :n 1 :min 0 :max 1 :replacement true) => success
        (g :n 1 :min 0 :max 1 :replacement false) => success
        (g :n 1 :min 0 :max 1 :replacement nil) => error
        (g :n 1 :min 0 :max 1 :replacement 0) => error
        (g :n 1 :min 0 :max 1 :replacement "") => error))
    (fact "base"
      (doseq [base [2 8 10 16]]
        (fact (g :n 1 :min 0 :max 1 :base base) => success))
      (doseq [base [1 3 5 7]]
        (fact (g :n 1 :min 0 :max 1 :base base) => error)))))

(fact "signed requests"
  (generate-integers :n 1 :min 0 :max 1 :signed true) => signed
  (generate-integers :n 1 :min 0 :max 1) =not=> signed)
