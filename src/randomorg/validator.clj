(ns randomorg.validator
  (:require [bouncer.core :as bouncer]
            [bouncer.validators :as v]))

(defn validate
  "Helper function build on top of bouncer validate.
   Throws exception when validation fails"
  [& args]
  (let [validate-map (apply bouncer/validate args)]
    (if-not (empty? (first validate-map))
      (throw (IllegalArgumentException. (str "Errors present: " validate-map))))
    ))

(v/defvalidator integer
  {:default-message-format "%s must be an integer"}
  [num]
  (integer? num))

(v/defvalidator boolean
  {:default-message-format "%s must be a boolean"}
  [value]
  (or (= false value)
      (= true value)))

(v/defvalidator string
  {:default-message-format "%s must be a string"}
  [value]
  (string? value))

(v/defvalidator ranged
  {:default-message-format "%s must be in a specified range"}
  [value [from to]]
  (<= from value to))

(v/defvalidator string-ranged
  {:default-message-format "%s must be in specified range"}
  [value [from to]]
  (<= from (count value) to))

(v/defvalidator byte
  {:default-message-format "%s must be divisible by 8"}
  [value]
  (zero? (mod value 8)))

(def n-validator [v/required integer [ranged [1 1e4]]])
(def n-uuid-validator [v/required integer [ranged [1 1e3]]])
(def range-1e9-validator [v/required integer [ranged [-1e9 1e9]]])
(def range-1e6-validator [v/required integer [ranged [-1e6 1e6]]])
(def decimal-range-validator [v/required integer [ranged [1 20]]])
(def string-length-validator [v/required integer [ranged [1 20]]])
(def significant-digits-validator [v/required integer [ranged [2 20]]])
(def base-validator [[v/member #{2 8 10 16} :message "base must be 2, 8, 10 or 16"]])
(def characters-validator [v/required string [string-ranged [1 80]]])
(def n-blob-validator [v/required integer [ranged [1 100]]])
(def blob-size-validator [v/required integer byte [ranged [1 1048576]] ])
(def blob-format-validator [[v/member #{"base64" "hex"} :message "format must be base64 or hex"]])
;; todo required random
(def required-random [v/required])
;; todo validate signature
(def required-signature [v/required string])
