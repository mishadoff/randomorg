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

(v/defvalidator ranged
  {:default-message-format "%s must be in specified range"}
  [value [from to]]
  (<= from value to))

(def n-validator [v/required integer [ranged [1 1e4]]])
(def range-validator [v/required integer [ranged [-1e9 1e9]]])
(def base-validator [[v/member #{2 8 10 16} :message "base must be 2, 8, 10 or 16"]])
