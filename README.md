# randomorg

Random generator via atmospheric noise random.org

TODO
``` clojure
[randomorg "0.1.0"]
```

## Rationale

Are your random numbers really random? 

-- links

## Examples

``` clojure
;; generate two 6-sided dice rolls
(generate-integers :n 2 :min 1 :max 6)

;; shuffle 52-card deck
(generate-integers :n 52 :min 1 :max 52 :replacement false)

;; uniform distribution of 10 numbers in [0, 1]
(generate-decimal-fractions :n 10 :digits 2)

;; normal distribution
(generate-gaussians :n 10 :mean 0 :std 10 :digits 2)

;; new password alphanumeric 12 chars
(generate-strings :n 1 :length 12 :characters (str lowercase uppercase digits))

;; three UUIDs
(generate-uuids :n 3)

;; 1Kb of random garbage
(generate-blobs :n 1 :size (* 8 1024) :format "hex")
```

All request are made with default API key, which is shared.
Request your own API key for production purposes.

``` clojure
(binding [*API_KEY* "your-api-key"]
  (generate-integers :n 10 :min 0 :max 99))
```

## Digital Signing

Random.org provides signed version of its API.
Just add an optional parameter `:signed true`

``` clojure
(generate-uuids :n 1 :signed true)
```

Response map contains additional object `:signed`

``` clojure
{:signed {:random { <random data> }
          :signature "SHA512-json-random"}}
```

Using this object you can validate that results are really random and comes from random.org. Encode random object to json, compute SHA-512 and verify against signature. Also, you can use random.org api call for that.

``` clojure
(verify-signature {:random { <random data> }
                   :signature "SHA512-json-random"})
```

## Remaining quota

Every succesful response return `:usage` object as well

``` clojure
{:requestsLeft 4978, :bitsLeft 482078}
```

Besides, you can request quota without data

``` clojure
(get-usage)
```

## Validation

To avoid wasteful calls with invalid params to server, we utilize usage of [bouncer](https://github.com/leonardoborges/bouncer) validation library. It contains plenty checks if all required parameters specified, values in a allowed range, etc.

If validation error occurs `IllegalArgumentException` is thrown

TODO (use error object)

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
