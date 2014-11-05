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
        (g :n 10.2 :min 1 :max 10) => error)
      (fact "out of range"
        (g :n 0 :min 0 :max 10) => error
        (g :n 1e5 :min 0 :max 10) => error))
    (fact "min"
      (fact "integer"
        (g :n 1 :min 0.1 :max 10) => error
        (g :n 1 :min "2" :max 10) => error)
      (fact "out of range"
        (g :n 0 :min 1000000001 :max 1000000002) => error
        (g :n 1 :min -1000000001 :max 1) => error))
    (fact "max"
      (fact "integer"
        (g :n 1 :min 0 :max 1.2) => error
        (g :n 1 :min 0 :max "1") => error)
      (fact "out of range"
        (g :n 0 :min 1 :max 1000000001) => error
        (g :n 1 :min 0 :max -1000000001) => error))
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

(fact "generate-decimal-fractions"
  (let [g generate-decimal-fractions] ;; just an alias
    (fact "happy case"
      (g :n 1 :digits 1) => success
      (g :n 2 :digits 10) => success
      (g :n 10 :digits 20) => success)
    (fact "integration"
      (fact "three uniform distribution numbers [0, 1]"
        (g :n 3 :digits 2) => success))
    (fact "required params"
      (g) => error
      (g :n 1) => error
      (g :digits 2) => error
      (g :digits 2 :replacement false) => error
      (g :n 1 :replacement true) => error)
    (fact "n"
      (fact "integer"
        (g :n "1" :digits 2) => error
        (g :n 10.2 :digits 2) => error)
      (fact "out of range"
        (g :n 0 :digits 2) => error
        (g :n 1e5 :digits 2) => error))
    (fact "digits"
      (fact "integer"
        (g :n 1 :digits 0.1) => error
        (g :n 1 :digits "2") => error)
      (fact "out of range"
        (g :n 1 :digits 0) => error
        (g :n 1 :digits 21) => error))
    (fact "replacement"
      (fact "boolean"
        (g :n 1 :digits 2 :replacement true) => success
        (g :n 1 :digits 2 :replacement false) => success
        (g :n 1 :digits 2 :replacement nil) => error
        (g :n 1 :digits 2 :replacement 0) => error
        (g :n 1 :digits 2 :replacement "") => error))))

(fact "generate-gaussians"
  (let [g generate-gaussians] ;; just an alias
    (fact "happy case"
      (g :n 1 :mean 0 :std 10.0 :digits 10) => success
      (g :n 2 :mean -10 :std 10.0 :digits 10) => success
      (g :n 10 :mean 1e6 :std 1 :digits 2) => success)
    (fact "integration"
      (fact "three normal distribution numbers [0, ~1]"
        (g :n 3 :mean 0 :std 1.0 :digits 2) => success))
    (fact "required params"
      (g) => error
      (g :n 1) => error
      (g :digits 2) => error
      (g :n 1 :digits 2) => error
      (g :n 1 :digits 2 :mean 0.0) => error
      (g :n 1 :digits 2 :std 1.0) => error)
    (fact "n"
      (fact "integer"
        (g :n "1" :std 1.0 :mean 0 :digits 2) => error
        (g :n 10.2 :std 1.0 :mean 0 :digits 2) => error)
      (fact "out of range"
        (g :n 0 :std 1.0 :mean 0 :digits 2) => error
        (g :n 1e5 :std 1.0 :mean 0 :digits 2) => error))
    (fact "digits"
      (fact "integer"
        (g :n 1 :std 1.0 :mean 0 :digits 0.1) => error
        (g :n 1 :std 1.0 :mean 0 :digits "2") => error)
      (fact "out of range"
        (g :n 1 :std 1.0 :mean 0 :digits 0) => error
        (g :n 1 :std 1.0 :mean 0 :digits 1) => error
        (g :n 1 :std 1.0 :mean 0 :digits 2) => success
        (g :n 1 :std 1.0 :mean 0 :digits 20) => success
        (g :n 1 :std 1.0 :mean 0 :digits 21) => error))
    (fact "mean"
      (fact "number"
        (g :n 1 :std 1.0 :mean 0 :digits 2) => success
        (g :n 1 :std 1.0 :mean 0.5 :digits 2) => success
        (g :n 1 :std 1.0 :mean "1.0" :digits 2) => error)
      (fact "out of range"
        (g :n 1 :std 1.0 :mean 0 :digits 2) => success
        (g :n 1 :std 1.0 :mean 1e6 :digits 2) => success
        (g :n 1 :std 1.0 :mean -1e6 :digits 2) => success
        (g :n 1 :std 1.0 :mean 1e7 :digits 2) => error
        (g :n 1 :std 1.0 :mean -1e7 :digits 2) => error))
    (fact "std"
      (fact "number"
        (g :n 1 :std 1 :mean 0.0 :digits 2) => success
        (g :n 1 :std 1.5 :mean 0.0 :digits 2) => success
        (g :n 1 :std "1.0" :mean 0.0 :digits 2) => error)
      (fact "out of range"
        (g :n 1 :std 1.0 :mean 0 :digits 2) => success
        (g :n 1 :std 1e6 :mean 0 :digits 2) => success
        (g :n 1 :std -1e6 :mean 0 :digits 2) => success
        (g :n 1 :std 1e7 :mean 0 :digits 2) => error
        (g :n 1 :std -1e7 :mean 0 :digits 2) => error))
        ))

(fact "signed requests"
  (fact "generate-integers"
    (generate-integers :n 1 :min 0 :max 1 :signed true) => signed
    (generate-integers :n 1 :min 0 :max 1) =not=> signed)
  (fact "generate-decimal-fractions"
    (generate-decimal-fractions :n 1 :digits 2 :signed true) => signed
    (generate-decimal-fractions :n 1 :digits 2) =not=> signed)
  (fact "generate-gaussians"
    (generate-gaussians :n 1 :std 1.0 :mean 0.0 :digits 2 :signed true) => signed
    (generate-gaussians :n 1 :std 1.0 :mean 0.0 :digits 2) =not=> signed)
  )
