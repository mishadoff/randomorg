# randomorg

Random generator via atmospheric noise Random.org

## Example usage

``` clojure
(binding [*API_KEY* "your-api-key"]
  (generate-integers :n 10 :min 0 :max 99))
```

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
